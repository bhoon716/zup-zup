package bhoon.sugang_helper.domain.notification.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.notification.repository.NotificationHistoryRepository;
import bhoon.sugang_helper.domain.notification.response.NotificationHistoryResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final UserRepository userRepository;

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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
