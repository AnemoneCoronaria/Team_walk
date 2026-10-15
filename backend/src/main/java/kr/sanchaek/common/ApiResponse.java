package kr.sanchaek.common;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API 응답을 명세의 공통 형식으로 보냅니다.
 *
 * <pre>
 * ApiResponse.ok(resp, course);                       // 200 { "success": true, "data": course }
 * ApiResponse.created(resp, Map.of("courseId", 13));  // 201
 * ApiResponse.fail(resp, 404, "NOT_FOUND", "코스를 찾을 수 없습니다.");
 *
 * LoginRequest body = ApiResponse.readBody(req, LoginRequest.class);  // JSON 요청 읽기
 * </pre>
 *
 * 날짜는 LocalDateTime 으로 담으면 "2026-11-02T10:00:00" 형식으로 나갑니다.
 * (DAO에서 rs.getObject("meet_at", LocalDateTime.class) 로 꺼내기)
 * 값이 null 인 칸도 "mbti": null 처럼 빠지지 않고 나갑니다 (명세 예시와 같게).
 */
public final class ApiResponse {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 팀 공통 Gson. 직접 new Gson() 하지 말고 이걸 쓰세요 (날짜 형식, null 처리 때문) */
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(src.format(DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> parseDateTime(json.getAsString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, type, ctx) -> LocalDate.parse(json.getAsString()))
            .create();

    private ApiResponse() {
    }

    public static void ok(HttpServletResponse resp, Object data) throws IOException {
        send(resp, HttpServletResponse.SC_OK, success(data));
    }

    public static void created(HttpServletResponse resp, Object data) throws IOException {
        send(resp, HttpServletResponse.SC_CREATED, success(data));
    }

    public static void fail(HttpServletResponse resp, int status, String code, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", error);
        send(resp, status, body);
    }

    /** JSON 요청 본문을 객체로 읽기. 형식이 틀리면 null */
    public static <T> T readBody(HttpServletRequest req, Class<T> type) throws IOException {
        try {
            return GSON.fromJson(req.getReader(), type);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static Map<String, Object> success(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }

    private static void send(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(GSON.toJson(body));
    }

    private static LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text);
        } catch (RuntimeException e) {
            throw new JsonParseException("날짜 형식이 아닙니다: " + text, e);
        }
    }
}
