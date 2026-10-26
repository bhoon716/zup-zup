package bhoon.sugang_helper.admin.audit.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.admin.audit.dto.response.AdminActionLogResponse;
import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActionLogController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public ResponseEntity<Page<AdminActionLogResponse>> getActionLogs(
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        return ResponseEntity.ok(adminAuditService.getActionLogs(fixedPageable));
    }
}
