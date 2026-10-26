package bhoon.sugang_helper.identity.user.controller;

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
    public ResponseEntity<Void> sendTestNotification() {
        userService.sendTestNotification();
        return ResponseEntity.ok(null);
    }
}
