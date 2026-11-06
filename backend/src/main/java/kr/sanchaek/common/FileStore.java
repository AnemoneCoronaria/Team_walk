package kr.sanchaek.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
 *
 * // &#64;Transactional 안에서: 롤백되면 파일도 지움 / 커밋된 뒤에만 지움
 * fileStore.saveInTransaction(photo, path);
 * fileStore.deleteAfterCommit(oldPaths);
 * </pre>
 *
 * gpx/ 폴더는 비공개입니다. /files/gpx/... 로는 내려 주지 않고(WebConfig), 권한을 확인하는
 * /api/courses/{id}/gpx · /api/matches/{id}/gpx 에서 download(...) 로만 내려 줍니다.
 */
@Component
public class FileStore {

    private static final Logger log = LoggerFactory.getLogger(FileStore.class);

    /** /files/** 로 공개하지 않는 폴더 (업로드 폴더 기준) */
    private static final String PRIVATE_DIR = "gpx";

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
            deleteQuietly(relativePath);   // 쓰다 만 파일이 남지 않게
            throw new UncheckedIOException("파일 저장 실패: " + target, e);
        }
    }

    /**
     * 트랜잭션 안에서 저장. 커밋되지 않으면(롤백 · 저장 중 오류) 파일을 지운다.
     * 정리 작업을 저장 <b>전에</b> 등록하므로 쓰다 실패한 파일도 남지 않는다. 트랜잭션 밖이면 save 와 같음
     */
    public void saveInTransaction(MultipartFile file, String relativePath) {
        resolve(relativePath);   // 경로가 잘못됐으면 등록 전에 400
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteQuietly(relativePath);
                    }
                }
            });
        }
        save(file, relativePath);
    }

    /**
     * 트랜잭션이 커밋된 뒤에 파일들을 지운다 (트랜잭션 밖이면 바로). 롤백되면 안 지움.
     * 이미 커밋된 뒤라 실패해도 예외를 던지지 않고 로그만 남긴다
     */
    public void deleteAfterCommit(Collection<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return;
        }
        List<String> paths = List.copyOf(relativePaths);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            paths.forEach(this::deleteQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paths.forEach(FileStore.this::deleteQuietly);
            }
        });
    }

    /** delete 와 같지만 실패해도 예외 없이 로그만 */
    public void deleteQuietly(String relativePath) {
        try {
            delete(relativePath);
        } catch (RuntimeException e) {
            log.warn("파일 삭제 실패 (무시): {}", relativePath, e);
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

    /** /files/** 로 내려 주면 안 되는 파일인지 (gpx/ 폴더 안). 대소문자 · 심볼릭 링크를 실제 경로로 비교 */
    public boolean isPrivate(Path file) {
        try {
            Path privateDir = baseDir.resolve(PRIVATE_DIR);
            if (!Files.exists(privateDir)) {
                return false;
            }
            Path real = file.toAbsolutePath().normalize();
            if (Files.exists(real)) {
                real = real.toRealPath();
            }
            return real.startsWith(privateDir.toRealPath());
        } catch (IOException e) {
            return true;   // 판단할 수 없으면 공개하지 않음
        }
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
