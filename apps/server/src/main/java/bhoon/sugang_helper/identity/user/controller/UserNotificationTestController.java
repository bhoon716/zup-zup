package bhoon.sugang_helper.identity.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.identity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationTestController {

    private final UserService userService;

    @PostMapping("/test")
    public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
        userService.sendTestNotification();
        return CommonResponse.ok(null, "알림 테스트를 전송했습니다.");
    }
}
