package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.seatalert.notification.service.UserNotificationTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserNotificationTestController {

    private final UserNotificationTestService notificationTestService;

    @PostMapping({"/api/v1/notifications/test", "/api/v1/users/notifications/test"})
    public ResponseEntity<Void> sendTestNotification() {
        notificationTestService.sendTestNotification();
        return ResponseEntity.ok(null);
    }
}
