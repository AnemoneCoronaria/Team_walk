package kr.sanchaek.landmark;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    /** GPX 하나를 모든 랜드마크와 한 번에 비교: 경로의 어느 점이든 반경 안이면 스탬프 */
    @Transactional
    public VerifyResult verify(long userId, MultipartFile file) {
        Boolean agreed = landmarks.agreeLocationForUpdate(userId);
        if (!Boolean.TRUE.equals(agreed)) {
            throw new ApiException(403, "CONSENT_REQUIRED", "위치 정보 이용에 동의해야 인증할 수 있어요.");
        }
        List<List<Double>> path = gpx.parse(file).path();

        List<LandmarkRepository.Row> near = landmarks.landmarks(userId).stream()
                .filter(l -> path.stream().anyMatch(p -> Geo.meters(l.lat(), l.lng(), p.get(0), p.get(1)) <= l.radiusM()))
                .toList();
        if (near.isEmpty()) {
            return new VerifyResult("FAIL", "NOT_NEAR", List.of(), List.of(), List.of());
        }

        Set<String> badgesBefore = codes(badges(userId));
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
        List<BadgeRef> newBadges = badges(userId).stream()
                .filter(b -> !badgesBefore.contains(b.code()))
                .map(b -> new BadgeRef(b.code(), b.name()))
                .toList();
        return new VerifyResult("SUCCESS", null, newStamps, already, newBadges);
    }

    @Transactional(readOnly = true)
    public MyStamps myStamps(long userId) {
        List<LandmarkRepository.Stamp> stamps = landmarks.stamps(userId);
        return new MyStamps(stamps.size(), landmarks.countLandmarks(), stamps, badges(userId));
    }

    /** 배지는 저장하지 않고 스탬프 수 · 끝난 동행 참여 횟수로 계산 (Figma R2의 5개) */
    List<Badge> badges(long userId) {
        List<LocalDateTime> stamps = landmarks.stamps(userId).stream().map(LandmarkRepository.Stamp::stampedAt).toList();
        long total = landmarks.countLandmarks();
        List<LandmarkRepository.DoneMatch> done = landmarks.doneMatches(userId);
        List<LocalDateTime> matches = done.stream().map(LandmarkRepository.DoneMatch::meetAt).toList();
        List<LocalDateTime> exchanges = done.stream()
                .filter(m -> "EXCHANGE".equals(m.target())).map(LandmarkRepository.DoneMatch::meetAt).toList();

        List<Badge> badges = new ArrayList<>();
        addIfReached(badges, "FIRST_WALK", "첫 산책", stamps, 1);
        addIfReached(badges, "LANDMARK_3", "랜드마크 3곳", stamps, 3);
        if (total > 0) {
            addIfReached(badges, "SIHEUNG_MASTER", "시흥 산책 마스터", stamps, (int) total);
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

    private static Set<String> codes(List<Badge> badges) {
        return badges.stream().map(Badge::code).collect(Collectors.toSet());
    }
}
