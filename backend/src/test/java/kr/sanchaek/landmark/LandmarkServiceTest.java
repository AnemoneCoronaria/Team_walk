package kr.sanchaek.landmark;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import kr.sanchaek.common.ApiException;

@SpringBootTest
@Transactional
class LandmarkServiceTest {
    @Autowired LandmarkService service;
    @Autowired JdbcTemplate jdbc;

    /** 시드 코스 경로로 GPX 만들기 */
    private MockMultipartFile courseGpx(long courseId) {
        String json = jdbc.queryForObject("SELECT path_json FROM courses WHERE course_id=?", String.class, courseId);
        Matcher m = Pattern.compile("\\[\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\]").matcher(json);
        StringBuilder points = new StringBuilder();
        while (m.find()) {
            points.append("<trkpt lat='").append(m.group(1)).append("' lon='").append(m.group(2)).append("'/>");
        }
        String xml = "<gpx xmlns='http://www.topografix.com/GPX/1/1'><trk><trkseg>" + points + "</trkseg></trk></gpx>";
        return new MockMultipartFile("file", "walk.gpx", "application/gpx+xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> codes(List<LandmarkService.Badge> badges) {
        return badges.stream().map(LandmarkService.Badge::code).toList();
    }

    @Test void listsLandmarksWithMyStamps() {
        var anonymous = service.list(null).items();
        assertEquals(8, anonymous.size());
        assertTrue(anonymous.stream().noneMatch(LandmarkService.Landmark::stamped));
        assertEquals(List.of(37.354517, 126.709916), anonymous.get(0).point());
        var minseo = service.list(2L).items();
        assertEquals(3, minseo.stream().filter(LandmarkService.Landmark::stamped).count());
        assertNotNull(minseo.get(0).stampedAt());
    }

    @Test void calculatesBadgesFromStampsAndDoneMatches() {
        var minseo = service.myStamps(2);
        assertEquals(3, minseo.stampCount());
        assertEquals(8, minseo.totalLandmarks());
        assertEquals(List.of("FIRST_WALK", "LANDMARK_3", "FIRST_MATCH"), codes(minseo.badges()));
        assertEquals(minseo.stamps().get(2).stampedAt(), minseo.badges().get(1).earnedAt());
        assertEquals(List.of("FIRST_WALK"), codes(service.myStamps(4).badges()));
        assertEquals(List.of(), service.myStamps(5).badges());   // 동훈은 6번 동행을 참여 취소함
    }

    @Test void verifyGivesNewStampsAndBadges() {
        // 동언(4): 배곧생명공원 1곳 → 코스4 경로로 빨강등대 · 함상전망대 (선사유적공원 311m 는 반경 밖)
        var result = service.verify(4, courseGpx(4));
        assertEquals("SUCCESS", result.result());
        assertNull(result.reason());
        assertEquals(List.of(2L, 3L), result.newStamps().stream().map(LandmarkService.LandmarkRef::landmarkId).toList());
        assertEquals(List.of(), result.alreadyStamped());
        assertEquals(List.of(new LandmarkService.BadgeRef("LANDMARK_3", "랜드마크 3곳")), result.newBadges());
        assertEquals(3, service.myStamps(4).stampCount());

        var again = service.verify(4, courseGpx(4));
        assertEquals("FAIL", again.result());
        assertEquals("ALREADY_ALL", again.reason());
        assertEquals(2, again.alreadyStamped().size());
    }

    @Test void verifyFailsWhenNotNearOrAlreadyStampedOrNoConsent() {
        assertEquals("ALREADY_ALL", service.verify(2, courseGpx(4)).reason());
        var notNear = service.verify(4, courseGpx(2));
        assertEquals("FAIL", notNear.result());
        assertEquals("NOT_NEAR", notNear.reason());

        var consent = assertThrows(ApiException.class, () -> service.verify(3, courseGpx(4)));
        assertEquals(403, consent.getStatus());
        assertEquals("CONSENT_REQUIRED", consent.getCode());
        var bad = new MockMultipartFile("file", "walk.gpx", "application/gpx+xml", "<gpx/>".getBytes());
        assertEquals("INVALID_FORMAT", assertThrows(ApiException.class, () -> service.verify(4, bad)).getCode());
        // 명세(flow-stamp) 순서: 동의 확인이 파일 검사보다 먼저
        assertEquals("CONSENT_REQUIRED", assertThrows(ApiException.class, () -> service.verify(3, bad)).getCode());
    }
}
