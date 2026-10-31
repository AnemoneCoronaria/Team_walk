package kr.sanchaek.review;

import java.util.List;
import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import kr.sanchaek.common.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReviewController {
    public record Created(long reviewId) {}

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Created> create(@LoginUser Long userId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String content,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        var form = new ReviewService.NewReview(courseId, matchId, rating, content);
        return ApiResponse.ok(new Created(service.create(userId, form, photos)));
    }

    @GetMapping("/api/courses/{id}/reviews")
    public ApiResponse<ReviewRepository.ReviewPage> courseReviews(@PathVariable long id,
            @LoginUser(required = false) Long userId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "false") boolean photo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.courseReviews(id, userId, sort, photo, page, size));
    }

    @GetMapping("/api/users/me/reviews")
    public ApiResponse<PageResult<ReviewRepository.MyReview>> myReviews(@LoginUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.myReviews(userId, page, size));
    }

    @DeleteMapping("/api/reviews/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, @LoginUser Long userId) {
        service.delete(userId, id);
        return ApiResponse.ok();
    }
}
