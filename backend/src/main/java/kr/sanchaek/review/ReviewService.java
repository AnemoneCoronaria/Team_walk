package kr.sanchaek.review;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.common.PageResult;
import kr.sanchaek.course.CourseRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 후기 작성 (R1) · 코스 후기 목록 (C2, C4) · 내 후기 · 삭제 (P4) */
@Service
public class ReviewService {
    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024;
    private static final int MIN_CONTENT_LENGTH = 10;
    private static final int MAX_CONTENT_LENGTH = 20000;
    /** content 는 TEXT 컬럼(최대 65,535바이트). 이모지(4바이트)만 쓰면 20000자도 넘칠 수 있어서 바이트도 확인 */
    private static final int TEXT_MAX_BYTES = 65_535;

    private final ReviewRepository reviews;
    private final CourseRepository courses;
    private final FileStore files;

    public ReviewService(ReviewRepository reviews, CourseRepository courses, FileStore files) {
        this.reviews = reviews;
        this.courses = courses;
        this.files = files;
    }

    public record NewReview(Long courseId, Long matchId, Integer rating, String content) {}

    @Transactional
    public long create(long userId, NewReview form, List<MultipartFile> photoFiles) {
        if (form.courseId() == null) {
            throw ApiException.badRequest("코스를 골라 주세요.");
        }
        if (form.rating() == null || form.rating() < 1 || form.rating() > 5) {
            throw ApiException.badRequest("별점은 1~5점이에요.");
        }
        String content = form.content() == null ? "" : form.content().strip();
        int length = content.codePointCount(0, content.length());
        if (length < MIN_CONTENT_LENGTH) {
            throw ApiException.badRequest("후기는 10자 이상 써 주세요.");
        }
        if (length > MAX_CONTENT_LENGTH || utf8Bytes(content) > TEXT_MAX_BYTES) {
            throw ApiException.badRequest("후기는 20000자 이하로 써 주세요.");
        }
        List<Photo> photos = validPhotos(photoFiles);
        courses.checkVisible(form.courseId(), userId);
        if (form.matchId() != null) {
            checkMatch(userId, form.courseId(), form.matchId());
        }

        long reviewId;
        try {
            reviewId = reviews.insert(userId, form.courseId(), form.matchId(), form.rating(), content);
        } catch (DuplicateKeyException e) {   // 같은 동행에 동시에 두 번 누른 경우
            throw duplicate();
        }
        for (int i = 0; i < photos.size(); i++) {
            String path = "reviews/" + reviewId + "-" + (i + 1) + photos.get(i).extension();
            files.saveInTransaction(photos.get(i).file(), path);
            reviews.insertPhoto(reviewId, path, i + 1);
        }
        return reviewId;
    }

    @Transactional(readOnly = true)
    public ReviewRepository.ReviewPage courseReviews(long courseId, Long userId, String sort, boolean photoOnly,
            int page, int size) {
        String order = switch (sort) {
            case "recent" -> "r.created_at DESC";
            case "rating" -> "r.rating DESC, r.created_at DESC";
            default -> throw ApiException.badRequest("정렬 값을 확인해 주세요.");
        };
        PageResult.validate(page, size);
        courses.checkVisible(courseId, userId);
        PageResult<ReviewRepository.Review> result = reviews.courseReviews(courseId, order, photoOnly, page, size);
        return new ReviewRepository.ReviewPage(result.items(), page, size, result.total(), reviews.avgRating(courseId));
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewRepository.MyReview> myReviews(long userId, int page, int size) {
        PageResult.validate(page, size);
        return reviews.userReviews(userId, page, size);
    }

    @Transactional
    public void delete(long userId, long reviewId) {
        Long author = reviews.authorForUpdate(reviewId);
        if (author == null) {   // 관리자가 숨긴 후기도 목록처럼 없는 것으로 (신고 기록 · 중복 작성 막기 유지)
            throw ApiException.notFound("후기를 찾을 수 없어요.");
        }
        if (author != userId) {
            throw ApiException.forbidden("내가 쓴 후기만 지울 수 있어요.");
        }
        List<String> paths = reviews.photoPaths(reviewId);
        reviews.delete(reviewId);
        files.deleteAfterCommit(paths);
    }

    /** 동행으로 걸었을 때: 그 코스의 끝난 동행이고, 내가 끝까지 참여했어야 함 */
    private void checkMatch(long userId, long courseId, long matchId) {
        ReviewRepository.MatchInfo match = reviews.matchInfo(matchId, userId);
        if (match == null) {
            throw ApiException.notFound("동행을 찾을 수 없어요.");
        }
        if (match.courseId() != courseId) {
            throw ApiException.badRequest("이 코스로 잡힌 동행이 아니에요.");
        }
        if (!"JOINED".equals(match.participantStatus())) {
            throw ApiException.forbidden("참여한 동행에만 후기를 쓸 수 있어요.");
        }
        if (!"DONE".equals(match.status())) {
            throw ApiException.badRequest("끝난 동행에만 후기를 쓸 수 있어요.");
        }
        if (reviews.hasReviewForMatch(userId, matchId)) {
            throw duplicate();
        }
    }

    private static ApiException duplicate() {
        return ApiException.conflict("DUPLICATE", "이 동행에는 이미 후기를 썼어요.");
    }

    private record Photo(MultipartFile file, String extension) {}

    /** 최대 5장, 장당 5MB, jpg · png (확장자와 파일 앞부분을 함께 확인) */
    private static List<Photo> validPhotos(List<MultipartFile> files) {
        List<Photo> photos = new ArrayList<>();
        if (files == null) {
            return photos;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;   // 사진을 고르지 않은 빈 칸
            }
            if (file.getSize() > MAX_PHOTO_SIZE) {
                throw new ApiException(400, "TOO_LARGE", "사진은 한 장에 5MB 이하여야 해요.");
            }
            photos.add(new Photo(file, imageExtension(file)));
        }
        if (photos.size() > MAX_PHOTOS) {
            throw ApiException.badRequest("사진은 최대 5장까지 올릴 수 있어요.");
        }
        return photos;
    }

    private static String imageExtension(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw ApiException.badRequest("사진을 읽을 수 없어요.");
        }
        boolean jpeg = read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        boolean png = read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
        if (jpeg && (name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            return ".jpg";
        }
        if (png && name.endsWith(".png")) {
            return ".png";
        }
        throw new ApiException(400, "INVALID_FORMAT", "사진은 jpg · png 파일만 올릴 수 있어요.");
    }

    private static int utf8Bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
