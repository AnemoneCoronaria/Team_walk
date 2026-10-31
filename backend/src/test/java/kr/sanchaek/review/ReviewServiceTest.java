package kr.sanchaek.review;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import kr.sanchaek.common.ApiException;
import kr.sanchaek.common.FileStore;
import kr.sanchaek.course.CourseRepository;

@SpringBootTest
@Transactional
class ReviewServiceTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired ReviewRepository reviews;
    @Autowired CourseRepository courses;
    @Autowired JdbcTemplate jdbc;
    @TempDir Path uploadDir;

    ReviewService service() {
        return new ReviewService(reviews, courses, new FileStore(uploadDir.toString()));
    }

    @Test void writesMatchReviewWithPhotosAndUpdatesRating() throws Exception {
        // 승민(6)은 끝난 동행 6번(코스 3)에 참여했지만 아직 후기를 안 씀
        List<MultipartFile> photos = List.of(new MockMultipartFile("photos", "a.PNG", "image/png", PNG),
                new MockMultipartFile("photos", "", "application/octet-stream", new byte[0]),
                new MockMultipartFile("photos", "b.jpeg", "image/jpeg", JPG));
        long id = service().create(6, new ReviewService.NewReview(3L, 6L, 2, "  바람이 시원했어요 또 올게요  "), photos);

        var page = service().courseReviews(3, null, "recent", false, 1, 10);
        assertEquals(3, page.total());
        assertEquals(3.7, page.avgRating());
        var mine = page.items().get(0);
        assertEquals(id, mine.reviewId());
        assertEquals("바람이 시원했어요 또 올게요", mine.content());
        assertTrue(mine.withMatch());
        assertEquals(List.of("/files/reviews/" + id + "-1.png", "/files/reviews/" + id + "-2.jpg"), mine.photos());
        assertArrayEquals(JPG, Files.readAllBytes(uploadDir.resolve("reviews/" + id + "-2.jpg")));

        var again = assertThrows(ApiException.class,
                () -> service().create(6, new ReviewService.NewReview(3L, 6L, 5, "한 번 더 쓰려고 해요 ㅎㅎ"), null));
        assertEquals("DUPLICATE", again.getCode());
    }

    @Test void listsSortsFiltersAndHidesHiddenReviews() {
        var byRating = service().courseReviews(3, null, "rating", false, 1, 10);
        assertEquals(List.of(5, 4), byRating.items().stream().map(ReviewRepository.Review::rating).toList());
        var withPhoto = service().courseReviews(3, null, "recent", true, 1, 10);
        assertEquals(1, withPhoto.total());
        assertEquals(List.of("/files/reviews/1-1.jpg", "/files/reviews/1-2.jpg"), withPhoto.items().get(0).photos());
        var course1 = service().courseReviews(1, null, "recent", false, 1, 10);
        assertEquals(1, course1.total());
        assertEquals(4.0, course1.avgRating());
        assertEquals(1, service().courseReviews(3, null, "recent", false, 2, 1).items().size());
        assertThrows(ApiException.class, () -> service().courseReviews(3, null, "best", false, 1, 10));
        assertEquals(404, assertThrows(ApiException.class,
                () -> service().courseReviews(9999, null, "recent", false, 1, 10)).getStatus());
    }

    @Test void rejectsInvalidReviews() {
        record Case(long user, ReviewService.NewReview form, List<MockMultipartFile> photos, int status) {}
        var fakeJpg = new MockMultipartFile("photos", "x.jpg", "image/jpeg", "not an image".getBytes());
        var six = java.util.Collections.nCopies(6, new MockMultipartFile("photos", "p.png", "image/png", PNG));
        for (var c : List.of(
                new Case(6, new ReviewService.NewReview(null, null, 4, "열 글자 넘는 후기예요"), null, 400),
                new Case(6, new ReviewService.NewReview(3L, null, 6, "열 글자 넘는 후기예요"), null, 400),
                new Case(6, new ReviewService.NewReview(3L, null, 4, "짧아요"), null, 400),
                new Case(6, new ReviewService.NewReview(3L, null, 4, "열 글자 넘는 후기예요"), List.of(fakeJpg), 400),
                new Case(6, new ReviewService.NewReview(3L, null, 4, "열 글자 넘는 후기예요"), six, 400),
                new Case(6, new ReviewService.NewReview(9999L, null, 4, "열 글자 넘는 후기예요"), null, 404),
                new Case(6, new ReviewService.NewReview(1L, 6L, 4, "코스가 다른 동행이에요"), null, 400),
                new Case(5, new ReviewService.NewReview(3L, 6L, 4, "참여 취소한 동행이에요"), null, 403),
                new Case(2, new ReviewService.NewReview(2L, 4L, 4, "아직 안 끝난 동행이에요"), null, 403),
                new Case(3, new ReviewService.NewReview(2L, 4L, 4, "아직 안 끝난 동행이에요"), null, 400))) {
            var e = assertThrows(ApiException.class, () -> service().create(c.user(), c.form(),
                    c.photos() == null ? null : List.<MultipartFile>copyOf(c.photos())));
            assertEquals(c.status(), e.getStatus(), c.toString());
        }
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Long.class));
    }
}
