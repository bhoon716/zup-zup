package bhoon.sugang_helper.admin.audit.dto.response;

import bhoon.sugang_helper.admin.audit.enums.ActionType;
import bhoon.sugang_helper.admin.audit.enums.TargetType;
import java.time.LocalDateTime;




public record AdminActionLogResponse(
        Long id,
        Long adminId,
        ActionType actionType,
        TargetType targetType,
        Long targetId,
        AdminAuditMetadata metadata,
        LocalDateTime createdAt) {
}
