package kr.sanchaek.course;

import kr.sanchaek.common.ApiResponse;
import kr.sanchaek.common.LoginUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class GpxController {
    private final GpxService service;
    public GpxController(GpxService service) {
        this.service = service;
    }

    @PostMapping("/api/gpx/parse")
    public ApiResponse<GpxService.Result> parse(@LoginUser Long userId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.parse(file));
    }
}
