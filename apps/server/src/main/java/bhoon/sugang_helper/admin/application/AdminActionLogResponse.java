package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.feedback.domain.ActionType;
import bhoon.sugang_helper.feedback.domain.TargetType;
import java.time.LocalDateTime;

/**
 * 관리자에게 제공하는 최소화된 감사 로그 응답입니다.
 */
public record AdminActionLogResponse(
        Long id,
        Long adminId,
        ActionType actionType,
        TargetType targetType,
        Long targetId,
        AdminAuditMetadata metadata,
        LocalDateTime createdAt) {
}
