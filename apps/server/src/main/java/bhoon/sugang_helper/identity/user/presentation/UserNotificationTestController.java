package bhoon.sugang_helper.identity.user.presentation;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.identity.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API")
public class UserNotificationTestController {

    private final UserService userService;

    @Operation(summary = "내 알림 테스트 발송", description = "현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다. 10초 쿨타임이 적용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 테스트 발송 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "400", description = "활성화된 채널 없음", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "429", description = "쿨타임 제한", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    @PostMapping("/test")
    public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
        userService.sendTestNotification();
        return CommonResponse.ok(null, "알림 테스트를 전송했습니다.");
    }
}
