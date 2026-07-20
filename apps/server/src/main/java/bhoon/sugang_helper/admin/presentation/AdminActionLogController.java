package bhoon.sugang_helper.admin.presentation;

import bhoon.sugang_helper.admin.application.AdminActionLogResponse;
import bhoon.sugang_helper.admin.application.AdminAuditService;
import bhoon.sugang_helper.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 감사 로그를 조회하는 API입니다.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "Cookie")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Audit Logs", description = "관리자 액션 감사 로그 API")
public class AdminActionLogController {

    private final AdminAuditService adminAuditService;

    @Operation(summary = "관리자 액션 로그 조회", description = "관리자만 최소화된 감사 로그 전체를 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<AdminActionLogResponse>>> getActionLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        return CommonResponse.ok(adminAuditService.getActionLogs(pageable), "관리자 액션 로그입니다.");
    }
}
