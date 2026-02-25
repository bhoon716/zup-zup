package bhoon.sugang_helper.domain.notification.controller;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.response.NotificationHistoryResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final UserRepository userRepository;

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
    public ResponseEntity<CommonResponse<List<NotificationHistoryResponse>>> getMyNotificationHistory() {
        User user = getCurrentUser();
        List<NotificationHistoryResponse> histories = notificationHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationHistoryResponse::from)
                .collect(Collectors.toList());
        return CommonResponse.ok(histories, "전체 알림 수신 내역입니다.");
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
