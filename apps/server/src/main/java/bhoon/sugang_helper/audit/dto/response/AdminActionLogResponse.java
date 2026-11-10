package bhoon.sugang_helper.audit.dto.response;

import bhoon.sugang_helper.audit.entity.AdminActionLog;
import bhoon.sugang_helper.audit.enums.ActionType;
import bhoon.sugang_helper.audit.enums.TargetType;
import java.time.LocalDateTime;

public record AdminActionLogResponse(
        Long id,
        Long adminId,
        ActionType actionType,
        TargetType targetType,
        Long targetId,
        AdminAuditMetadata metadata,
        LocalDateTime createdAt) {

    public static AdminActionLogResponse from(AdminActionLog actionLog, AdminAuditMetadata metadata) {
        return new AdminActionLogResponse(
                actionLog.getId(),
                actionLog.getAdminId(),
                actionLog.getActionType(),
                actionLog.getTargetType(),
                actionLog.getTargetId(),
                metadata,
                actionLog.getCreatedAt());
    }
}
