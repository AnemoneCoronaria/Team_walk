package kr.sanchaek.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 업로드 폴더의 파일을 브라우저에 보내 줍니다.
 * GET /files/reviews/1-1.jpg → (업로드 폴더)/reviews/1-1.jpg
 *
 * React 개발 서버(5173)에서는 vite.config.js 의 '/files' 프록시를 거쳐 여기로 옵니다.
 */
@WebServlet("/files/*")
public class FileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo(); // "/reviews/1-1.jpg"
        Path file = pathInfo == null ? null : FileStore.resolve(pathInfo.substring(1));
        if (file == null || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String type = getServletContext().getMimeType(file.getFileName().toString());
        resp.setContentType(type != null ? type : "application/octet-stream");
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("Cache-Control", "max-age=3600");
        Files.copy(file, resp.getOutputStream());
    }
}
