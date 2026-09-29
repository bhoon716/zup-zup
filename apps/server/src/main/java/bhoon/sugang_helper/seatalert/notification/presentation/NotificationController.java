package bhoon.sugang_helper.seatalert.notification.presentation;
import bhoon.sugang_helper.seatalert.notification.application.NotificationHistoryResponse;


import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.seatalert.notification.contract.NotificationHistoryQuery;
import bhoon.sugang_helper.identity.user.contract.CurrentUserNotificationTester;
import bhoon.sugang_helper.identity.user.contract.UserIdentity;
import bhoon.sugang_helper.identity.user.contract.UserIdentityQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    private final NotificationHistoryQuery notificationHistoryQuery;
    private final UserIdentityQuery userIdentityQuery;
    private final CurrentUserNotificationTester currentUserNotificationTester;

    /**
     * 현재 로그인한 사용자의 전체 알림 수신 내역을 조회합니다.
     */
    @Operation(summary = "내 알림 수신 내역 조회", description = "현재 로그인한 사용자의 전체 알림 수신 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 내역 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "전체 알림 수신 내역입니다.",
                      "data": [
                        {
                          "id": 1,
                          "courseKey": "CLTR.0031-01",
                          "title": "공석 발생",
                          "message": "[기초프로그래밍] 과목에 공석이 발생했습니다.",
                          "channel": "FCM",
                          "sentAt": "2024-01-01T12:00:00"
                        }
                      ]
                    }
                    """)))
    })
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<Slice<NotificationHistoryResponse>>> getMyNotificationHistory(
            @PageableDefault(size = 30) Pageable pageable) {
        UserIdentity user = getCurrentUser();
        Pageable boundedPageable = PageableGuard.requireBounded(pageable, MAX_HISTORY_PAGE_SIZE, 10_000);
        Slice<NotificationHistoryResponse> histories = notificationHistoryQuery
                .findForUser(user.id(), boundedPageable)
                .map(NotificationHistoryResponse::from);
        return CommonResponse.ok(histories, "전체 알림 수신 내역입니다.");
    }

    /**
     * 현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다.
     */
    @Operation(summary = "내 알림 테스트 발송", description = "현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다. 10초 쿨타임이 적용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 테스트 발송 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "400", description = "활성화된 채널 없음", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "429", description = "쿨타임 제한", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    @PostMapping("/test")
    public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
        currentUserNotificationTester.sendTestNotification();
        return CommonResponse.ok(null, "알림 테스트를 전송했습니다.");
    }

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     */
    private UserIdentity getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
