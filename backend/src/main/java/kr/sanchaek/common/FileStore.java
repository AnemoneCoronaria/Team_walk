package kr.sanchaek.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import jakarta.servlet.http.Part;

/**
 * 업로드 폴더(후기 사진)에 파일 저장 · 주소 만들기.
 *
 * 폴더 위치는 db.properties 의 upload.dir (윈도우 기본 C:/dev/jspTeam/uploads).
 * DB에는 "reviews/88-1.jpg" 같은 상대 경로만 넣고, 응답에는 url(...) 로 "/files/reviews/88-1.jpg" 를 보냅니다.
 *
 * <pre>
 * String path = "reviews/" + reviewId + "-" + order + ".jpg";
 * FileStore.save(part, path);            // 파일 저장
 * // INSERT INTO review_photos (review_id, file_path, sort_order) VALUES (?, path, ?)
 * String url = FileStore.url(path);      // 응답용 "/files/reviews/88-1.jpg"
 * </pre>
 */
public final class FileStore {

    private FileStore() {
    }

    /** 업로드 폴더 (절대 경로) */
    public static Path baseDir() {
        return Paths.get(AppConfig.get("upload.dir", "C:/dev/jspTeam/uploads")).toAbsolutePath().normalize();
    }

    /** 업로드된 파일을 업로드 폴더 아래 relativePath 에 저장 */
    public static void save(Part part, String relativePath) throws IOException {
        Path target = resolve(relativePath);
        if (target == null) {
            throw new IOException("잘못된 저장 경로: " + relativePath);
        }
        Files.createDirectories(target.getParent());
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 실제 파일 지우기 (후기 삭제 시). 없으면 무시 */
    public static void delete(String relativePath) throws IOException {
        Path target = resolve(relativePath);
        if (target != null) {
            Files.deleteIfExists(target);
        }
    }

    /** DB 경로 → 브라우저 주소 */
    public static String url(String relativePath) {
        return "/files/" + relativePath;
    }

    /** 상대 경로 → 업로드 폴더 안의 실제 경로. 폴더 밖을 가리키면(../ 등) null */
    static Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path base = baseDir();
        Path target = base.resolve(relativePath).normalize();
        return target.startsWith(base) && !target.equals(base) ? target : null;
    }
}
