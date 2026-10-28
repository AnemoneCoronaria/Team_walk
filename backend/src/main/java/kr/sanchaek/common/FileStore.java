package kr.sanchaek.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 폴더(후기 사진 · GPX)에 파일 저장 · 삭제 · 다운로드.
 *
 * 폴더 위치는 application-local.properties 의 app.upload-dir (예: C:/dev/jspTeam/uploads).
 * DB에는 "reviews/88-1.jpg" 같은 <b>폴더 기준 상대 경로</b>만 넣고, 응답에는 url(...) 로 "/files/reviews/88-1.jpg" 를 보냅니다.
 * /files/... 주소로 파일을 내려 주는 건 WebConfig 가 합니다.
 *
 * <pre>
 * private final FileStore fileStore;   // 생성자로 받기
 *
 * // 저장 (후기 사진, 동언)
 * String path = "reviews/" + reviewId + "-" + order + ".jpg";
 * fileStore.save(photo, path);                 // photo 는 @RequestPart MultipartFile
 * // INSERT INTO review_photos (review_id, file_path, sort_order) VALUES (?, ?, ?)  ← path 를 저장
 * String url = FileStore.url(path);            // 응답용 "/files/reviews/88-1.jpg"
 *
 * // GPX 다운로드 (민서 · 동언)
 * return fileStore.download(course.gpxPath(), "옥구공원-둘레길.gpx");
 * </pre>
 */
@Component
public class FileStore {

    private final Path baseDir;

    /** app.upload-dir 이 없으면 서버를 켠 폴더의 uploads/ (backend/uploads, 깃허브에는 안 올라감) */
    public FileStore(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 폴더를 만들 수 없습니다: " + baseDir
                    + " (application-local.properties 의 app.upload-dir 확인)", e);
        }
    }

    /** 업로드 폴더 (절대 경로) */
    public Path baseDir() {
        return baseDir;
    }

    /** 업로드된 파일을 업로드 폴더 아래 relativePath 에 저장. 같은 이름이 있으면 덮어씀 */
    public void save(MultipartFile file, String relativePath) {
        Path target = resolve(relativePath);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + target, e);
        }
    }

    /** 실제 파일 지우기 (후기 · 코스 삭제 시). 없으면 무시 */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제 실패: " + relativePath, e);
        }
    }

    /** 저장된 파일을 다운로드 응답으로 (GPX 받기). 파일이 없으면 404 */
    public ResponseEntity<Resource> download(String relativePath, String downloadName) {
        if (relativePath == null || relativePath.isBlank() || !Files.isRegularFile(resolve(relativePath))) {
            throw ApiException.notFound("파일을 찾을 수 없어요.");
        }
        Path file = resolve(relativePath);
        MediaType type = relativePath.endsWith(".gpx")
                ? MediaType.parseMediaType("application/gpx+xml")
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(downloadName, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(file));
    }

    /** DB 경로 → 브라우저 주소. null 이면 null */
    public static String url(String relativePath) {
        return relativePath == null ? null : "/files/" + relativePath;
    }

    /** 상대 경로 → 업로드 폴더 안의 실제 경로. 폴더 밖(../ 등)을 가리키면 오류 */
    private Path resolve(String relativePath) {
        Path target = baseDir.resolve(relativePath).normalize();
        if (!target.startsWith(baseDir) || target.equals(baseDir)) {
            throw ApiException.badRequest("잘못된 파일 경로예요.");
        }
        return target;
    }
}
