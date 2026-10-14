package kr.sanchaek.common;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 환경 테스트용 API. GET /api/hello
 * 응답 형식은 API 명세의 공통 규칙({ success, data })을 따른다.
 */
@WebServlet("/api/hello")
public class HelloServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "안녕하세요, 산책시루 서버입니다");
        data.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        data.put("javaVersion", System.getProperty("java.version"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);

        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(gson.toJson(body));
    }
}