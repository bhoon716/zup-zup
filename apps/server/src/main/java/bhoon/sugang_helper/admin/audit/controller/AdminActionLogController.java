package bhoon.sugang_helper.admin.audit.controller;

import bhoon.sugang_helper.admin.audit.dto.response.AdminActionLogResponse;
import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import bhoon.sugang_helper.common.response.CommonResponse;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminActionLogController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public ResponseEntity<CommonResponse<Page<AdminActionLogResponse>>> getActionLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        return CommonResponse.ok(adminAuditService.getActionLogs(pageable), "관리자 액션 로그입니다.");
    }
}
