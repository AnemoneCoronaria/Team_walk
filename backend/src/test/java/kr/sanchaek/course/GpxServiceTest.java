package kr.sanchaek.course;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import kr.sanchaek.common.ApiException;

class GpxServiceTest {
    private final GpxService service = new GpxService();
    private MockMultipartFile file(String xml) { return new MockMultipartFile("file", "route.gpx", "application/gpx+xml", xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    @Test void parsesLargeTrackWithTrailingExtensionsPromptly() {
        String points = "<trkpt lat='37.35' lon='126.7'/><trkpt lat='37.351' lon='126.7'/>".repeat(20000);
        var upload = file("<gpx><trk><trkseg>" + points + "</trkseg></trk><extensions>"
                + "<extra/>".repeat(40000) + "</extensions></gpx>");
        assertTrue(upload.getSize() < 5 * 1024 * 1024);
        var result = assertTimeout(java.time.Duration.ofSeconds(5), () -> service.parse(upload));
        assertEquals(40000, result.pointCount());
    }

    @Test void parsesRoute() {
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < 25; i++) points.append("<trkpt lat='37.35272' lon='").append(126.709915 + i * 0.0002).append("'/>");
        var result = service.parse(file("<gpx xmlns='http://www.topografix.com/GPX/1/1'><trk><trkseg>" + points + "</trkseg></trk></gpx>"));
        assertEquals(25, result.pointCount());
        assertEquals("OKGU", result.area());
        assertTrue(result.distanceKm() >= 0.3);
        assertEquals(result.path().get(0), result.start());
    }
    @Test void rejectsInvalidAndShortFiles() {
        assertEquals("INVALID_FORMAT", assertThrows(ApiException.class, () -> service.parse(file("<gpx/>"))).getCode());
        assertEquals("INVALID_FORMAT", assertThrows(ApiException.class, () -> service.parse(file("<!DOCTYPE gpx [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><gpx>&x;</gpx>"))).getCode());
        assertEquals("TOO_SHORT", assertThrows(ApiException.class, () -> service.parse(file("<gpx><trk><trkseg><trkpt lat='37' lon='126'/></trkseg></trk></gpx>"))).getCode());
        assertEquals("INVALID_FORMAT", assertThrows(ApiException.class, () -> service.parse(file("<gpx><trk><trkseg><trkpt lat='91' lon='126'/></trkseg></trk></gpx>"))).getCode());
    }
    @Test void distanceDoesNotBridgeSegments() {
        String point1 = "<trkpt lat='37' lon='126'/>".repeat(10);
        String point2 = "<trkpt lat='38' lon='126'/>".repeat(10);
        assertEquals("TOO_SHORT", assertThrows(ApiException.class, () -> service.parse(file("<gpx><trk><trkseg>" + point1 + "</trkseg><trkseg>" + point2 + "</trkseg></trk></gpx>"))).getCode());
    }
}
