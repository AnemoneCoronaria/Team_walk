package kr.sanchaek.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import kr.sanchaek.common.ApiExceptionHandler;
import kr.sanchaek.common.LoginUserArgumentResolver;
import kr.sanchaek.common.SessionUtil;

class GpxControllerTest {
    @Test void requiresSessionAndReturnsParsedRoute() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new GpxController(new GpxService()))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver()).setControllerAdvice(new ApiExceptionHandler()).build();
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < 25; i++) points.append("<trkpt lat='37.35272' lon='").append(126.709915 + i * 0.0002).append("'/>");
        byte[] xml = ("<gpx><trk><trkseg>" + points + "</trkseg></trk></gpx>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "route.gpx", "application/gpx+xml", xml);
        mvc.perform(multipart("/api/gpx/parse").file(file)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        var session = new MockHttpSession();
        session.setAttribute(SessionUtil.USER_ID, 4L);
        mvc.perform(multipart("/api/gpx/parse").file(file).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointCount").value(25)).andExpect(jsonPath("$.data.area").value("OKGU"));
    }
}
