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

    /**
     * 관리자 페이지에서 필요한 개요와 크롤링 타겟 정보를 한 번에 조회합니다.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardSnapshotResponse> getDashboardSnapshot() {
        AdminDashboardSnapshotResponse response = adminService.getDashboardSnapshot();
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 관리자의 알림 설정에 맞춰 테스트 알림을 발송합니다.
     */
    @PostMapping("/notifications/test")
    public ResponseEntity<Void> sendTestNotification() {
        adminService.sendTestNotification();
        return ResponseEntity.ok(null);
    }
}
