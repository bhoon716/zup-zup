package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.request.UserDeviceRequest;
import bhoon.sugang_helper.domain.user.response.UserDeviceResponse;
import bhoon.sugang_helper.domain.user.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/devices")
@RequiredArgsConstructor
@Tag(name = "User Device", description = "사용자 기기 관련 API")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @Operation(summary = "기기 목록 조회", description = "사용자의 등록된 기기 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<UserDeviceResponse>>> getUserDevices() {
        User user = userDeviceService.getCurrentUserOrThrow();
        return ResponseEntity.ok(
                CommonResponse.success(userDeviceService.getUserDevices(user.getId()), "기기 목록을 조회했습니다."));
    }

    @Operation(summary = "기기 등록", description = "푸시 알림을 위한 FCM 토큰을 등록합니다. (별칭 포함)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "등록 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "기기가 성공적으로 등록되었습니다.",
                      "data": null
                    }
                    """)))
    })
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> registerDevice(@Valid @RequestBody UserDeviceRequest request) {
        userDeviceService.registerDevice(request);
        return CommonResponse.ok(null, "기기가 성공적으로 등록되었습니다.");
    }

    @Operation(summary = "기기 해제 (ID 기반)", description = "등록된 기기를 ID로 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteDevice(@PathVariable Long id) {
        userDeviceService.deleteDeviceById(id);
        return CommonResponse.ok(null, "기기가 삭제되었습니다.");
    }

    @Operation(summary = "기기 해제 (토큰 기반) - Deprecated", description = "등록된 FCM 토큰을 해제합니다. (ID 기반 삭제 권장)")
    @DeleteMapping("/token/{token}")
    public ResponseEntity<CommonResponse<Void>> unregisterDevice(@PathVariable String token) {
        userDeviceService.unregisterDevice(token);
        return CommonResponse.ok(null, "기기 등록이 해제되었습니다.");
    }
}
