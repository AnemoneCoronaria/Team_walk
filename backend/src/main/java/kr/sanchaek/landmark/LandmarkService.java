package kr.sanchaek.landmark;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.course.Geo;
import kr.sanchaek.course.GpxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 랜드마크 목록 · GPX 인증 · 스탬프 · 배지 (R1, R1-2, R2, R2-1) */
@Service
public class LandmarkService {
    public record Landmark(long landmarkId, String name, String area, List<Double> point, int radiusM,
            boolean stamped, LocalDateTime stampedAt) {}
    public record Landmarks(List<Landmark> items) {}

    public record LandmarkRef(long landmarkId, String name) {}
    public record BadgeRef(String code, String name) {}
    /** 실패(R1-2)도 200 으로 보낸다. reason 은 실패일 때만 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VerifyResult(String result, String reason, List<LandmarkRef> newStamps,
            List<LandmarkRef> alreadyStamped, List<BadgeRef> newBadges) {}

    public record Badge(String code, String name, LocalDateTime earnedAt) {}
    public record MyStamps(int stampCount, long totalLandmarks, List<LandmarkRepository.Stamp> stamps,
            List<Badge> badges) {}

    private final LandmarkRepository landmarks;
    private final GpxService gpx;

    public LandmarkService(LandmarkRepository landmarks, GpxService gpx) {
        this.landmarks = landmarks;
        this.gpx = gpx;
    }

    @Transactional(readOnly = true)
    public Landmarks list(Long userId) {
        return new Landmarks(landmarks.landmarks(userId).stream()
                .map(l -> new Landmark(l.landmarkId(), l.name(), l.area(), List.of(l.lat(), l.lng()), l.radiusM(),
                        l.stampedAt() != null, l.stampedAt()))
                .toList());
    }

    /**
     * GPX 하나를 모든 랜드마크와 한 번에 비교: 경로의 어느 점이든 반경 안이면 스탬프.
     * 동의 확인 → 파일 검사 순서는 명세(flow-stamp)대로. 파일 검사(최대 5MB)는 회원 행을 잠그기 전에 끝낸다.
     */
    @Transactional
    public VerifyResult verify(long userId, MultipartFile file) {
        checkConsent(landmarks.agreeLocation(userId));
        List<List<Double>> path = gpx.parse(file).path();
        checkConsent(landmarks.agreeLocationForUpdate(userId));

        // 잠근 뒤에 읽어야 동시에 들어온 인증과 스탬프 수가 엇갈리지 않는다. 모든 랜드마크가 들어 있어 전체 수로도 쓴다
        List<LandmarkRepository.Row> all = landmarks.landmarks(userId);
        List<LandmarkRepository.Row> near = all.stream()
                .filter(l -> Geo.passesNear(path, l.lat(), l.lng(), l.radiusM()))
                .toList();
        if (near.isEmpty()) {
            return new VerifyResult("FAIL", "NOT_NEAR", List.of(), List.of(), List.of());
        }

        int stampsBefore = (int) all.stream().filter(l -> l.stampedAt() != null).count();
        List<LandmarkRef> newStamps = new ArrayList<>();
        List<LandmarkRef> already = new ArrayList<>();
        for (LandmarkRepository.Row l : near) {
            LandmarkRef ref = new LandmarkRef(l.landmarkId(), l.name());
            if (l.stampedAt() == null && landmarks.addStamp(userId, l.landmarkId())) {
                newStamps.add(ref);
            } else {
                already.add(ref);
            }
        }
        if (newStamps.isEmpty()) {
            return new VerifyResult("FAIL", "ALREADY_ALL", List.of(), already, List.of());
        }
        // 인증으로 바뀌는 건 스탬프 수뿐이라 동행 배지는 다시 셀 필요가 없다
        int stampsAfter = stampsBefore + newStamps.size();
        List<BadgeRef> newBadges = stampBadges(all.size()).stream()
                .filter(b -> b.count() > stampsBefore && b.count() <= stampsAfter)
                .map(b -> new BadgeRef(b.code(), b.name()))
                .toList();
        return new VerifyResult("SUCCESS", null, newStamps, already, newBadges);
    }

    private static void checkConsent(Boolean agreed) {
        if (!Boolean.TRUE.equals(agreed)) {
            throw new ApiException(403, "CONSENT_REQUIRED", "위치 정보 이용에 동의해야 인증할 수 있어요.");
        }
    }

    @Transactional(readOnly = true)
    public MyStamps myStamps(long userId) {
        List<LandmarkRepository.Stamp> stamps = landmarks.stamps(userId);
        long total = landmarks.countLandmarks();
        return new MyStamps(stamps.size(), total, stamps, badges(stamps, total, landmarks.doneMatches(userId)));
    }

    /** 스탬프 수로 받는 배지. count 번째 스탬프를 받을 때 생긴다 */
    private record StampBadge(String code, String name, int count) {}

    private static List<StampBadge> stampBadges(long totalLandmarks) {
        List<StampBadge> list = new ArrayList<>();
        list.add(new StampBadge("FIRST_WALK", "첫 산책", 1));
        list.add(new StampBadge("LANDMARK_3", "랜드마크 3곳", 3));
        if (totalLandmarks > 0) {
            list.add(new StampBadge("SIHEUNG_MASTER", "시흥 산책 마스터", (int) totalLandmarks));
        }
        return list;
    }

    /** 배지는 저장하지 않고 스탬프 수 · 끝난 동행 참여 횟수로 계산 (Figma R2의 5개). stamps 는 받은 순서대로 */
    static List<Badge> badges(List<LandmarkRepository.Stamp> stamps, long totalLandmarks,
            List<LandmarkRepository.DoneMatch> done) {
        List<LocalDateTime> stampTimes = stamps.stream().map(LandmarkRepository.Stamp::stampedAt).toList();
        List<LocalDateTime> matches = done.stream().map(LandmarkRepository.DoneMatch::meetAt).toList();
        List<LocalDateTime> exchanges = done.stream()
                .filter(LandmarkRepository.DoneMatch::exchange).map(LandmarkRepository.DoneMatch::meetAt).toList();

        List<Badge> badges = new ArrayList<>();
        for (StampBadge b : stampBadges(totalLandmarks)) {
            addIfReached(badges, b.code(), b.name(), stampTimes, b.count());
        }
        addIfReached(badges, "FIRST_MATCH", "첫 동행", matches, 1);
        addIfReached(badges, "EXCHANGE_3", "내 · 외국인 교류 3회", exchanges, 3);
        return badges;
    }

    /** times 가 n 개 이상이면 n 번째 시각을 earnedAt 으로 */
    private static void addIfReached(List<Badge> badges, String code, String name, List<LocalDateTime> times, int n) {
        if (times.size() >= n) {
            badges.add(new Badge(code, name, times.get(n - 1)));
        }
    }
}
