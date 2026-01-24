package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.request.UserDeviceRequest;
import bhoon.sugang_helper.domain.user.service.UserDeviceService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/devices")
@RequiredArgsConstructor
@Tag(name = "User Device", description = "사용자 기기 관련 API")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @Operation(summary = "기기 등록", description = "푸시 알림을 위한 FCM 토큰을 등록합니다.")
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

    @Operation(summary = "기기 해제", description = "등록된 FCM 토큰을 해제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "해제 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "기기 등록이 해제되었습니다.",
                      "data": null
                    }
                    """)))
    })
    @DeleteMapping("/{token}")
    public ResponseEntity<CommonResponse<Void>> unregisterDevice(@PathVariable String token) {
        userDeviceService.unregisterDevice(token);
        return CommonResponse.ok(null, "기기 등록이 해제되었습니다.");
    }
}
