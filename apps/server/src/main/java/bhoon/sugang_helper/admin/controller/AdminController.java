package bhoon.sugang_helper.admin.controller;

import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.admin.service.AdminService;
import bhoon.sugang_helper.seatalert.api.NotificationTraffic;
import bhoon.sugang_helper.seatalert.api.NotificationTraffic.Bucket;
import bhoon.sugang_helper.seatalert.api.NotificationTraffic.Range;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final NotificationTraffic notificationTraffic;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardSnapshotResponse> getDashboardSnapshot() {
        AdminDashboardSnapshotResponse response = adminService.getDashboardSnapshot();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications/traffic")
    public List<Bucket> getNotificationTraffic(@RequestParam Range range) {
        return notificationTraffic.findRecent(range);
    }

    @PostMapping("/notifications/test")
    public ResponseEntity<Void> sendTestNotification() {
        adminService.sendTestNotification();
        return ResponseEntity.ok(null);
    }
}
