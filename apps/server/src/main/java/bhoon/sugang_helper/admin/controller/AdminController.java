package bhoon.sugang_helper.admin.controller;

import bhoon.sugang_helper.admin.dto.response.AdminDashboardSnapshotResponse;
import bhoon.sugang_helper.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;




    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardSnapshotResponse> getDashboardSnapshot() {
        AdminDashboardSnapshotResponse response = adminService.getDashboardSnapshot();
        return ResponseEntity.ok(response);
    }




    @PostMapping("/notifications/test")
    public ResponseEntity<Void> sendTestNotification() {
        adminService.sendTestNotification();
        return ResponseEntity.ok(null);
    }
}
