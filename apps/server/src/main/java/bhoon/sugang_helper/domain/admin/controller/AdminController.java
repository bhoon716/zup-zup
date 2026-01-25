package bhoon.sugang_helper.domain.admin.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.admin.response.AdminDashboardResponse;
import bhoon.sugang_helper.domain.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
