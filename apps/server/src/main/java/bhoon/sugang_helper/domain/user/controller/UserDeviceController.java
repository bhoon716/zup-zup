package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.request.UserDeviceRequest;
import bhoon.sugang_helper.domain.user.service.UserDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @PostMapping
    public ResponseEntity<CommonResponse<Void>> registerDevice(@Valid @RequestBody UserDeviceRequest request) {
        userDeviceService.registerDevice(request);
        return CommonResponse.ok(null, "기기가 성공적으로 등록되었습니다.");
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<CommonResponse<Void>> unregisterDevice(@PathVariable String token) {
        userDeviceService.unregisterDevice(token);
        return CommonResponse.ok(null, "기기 등록이 해제되었습니다.");
    }
}
