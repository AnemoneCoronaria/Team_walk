package kr.sanchaek.landmark;

import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class LandmarkController {
    private final LandmarkService service;

    public LandmarkController(LandmarkService service) {
        this.service = service;
    }

    @GetMapping("/api/landmarks")
    public ApiResponse<LandmarkService.Landmarks> list(@LoginUser(required = false) Long userId) {
        return ApiResponse.ok(service.list(userId));
    }

    @PostMapping("/api/landmarks/verify")
    public ApiResponse<LandmarkService.VerifyResult> verify(@LoginUser Long userId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.verify(userId, file));
    }

    @GetMapping("/api/users/me/stamps")
    public ApiResponse<LandmarkService.MyStamps> myStamps(@LoginUser Long userId) {
        return ApiResponse.ok(service.myStamps(userId));
    }
}
