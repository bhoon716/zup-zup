package bhoon.sugang_helper.domain.admin.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.admin.request.TestNotificationRequest;
import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "관리자 대시보드 통계", description = "전체 서비스 현황 통계를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<AdminDashboardResponse>> getDashboardStats() {
        AdminDashboardResponse response = adminService.getDashboardStats();
        return CommonResponse.ok(response, "관리자 대시보드 통계 정보입니다.");
    }

    @Operation(summary = "알림 테스트 전송", description = "특정 사용자에게 테스트 알림을 전송합니다.")
    @PostMapping("/notifications/test")
    public ResponseEntity<CommonResponse<Void>> sendTestNotification(
            @RequestBody @Valid TestNotificationRequest request) {
        adminService.sendTestNotification(request);
        return CommonResponse.ok(null, "테스트 알림이 성공적으로 요청되었습니다.");
    }
}
