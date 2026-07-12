package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.feedback.domain.ActionType;
import bhoon.sugang_helper.feedback.domain.AdminActionLog;
import bhoon.sugang_helper.feedback.domain.AdminActionLogRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.TargetType;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.user.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 민감한 관리자 조작을 동일한 최소화된 형식으로 감사합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;
    private final AdminAuditContentFingerprint contentFingerprint;

    @Transactional
    public void recordStatusChange(User admin, Long feedbackId, FeedbackStatus previousStatus, FeedbackStatus newStatus) {
        record(admin, ActionType.STATUS_CHANGE, TargetType.FEEDBACK, feedbackId,
                AdminAuditMetadata.statusChanged(previousStatus, newStatus));
    }

    @Transactional
    public void recordReplyCreated(User admin, Long replyId, Long feedbackId, String content) {
        record(admin, ActionType.REPLY_CREATE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyCreated(feedbackId, contentFingerprint.summarize(content)));
    }

    @Transactional
    public void recordReplyUpdated(User admin, Long replyId, Long feedbackId, String beforeContent, String afterContent) {
        record(admin, ActionType.REPLY_UPDATE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyUpdated(
                        feedbackId,
                        contentFingerprint.summarize(beforeContent),
                        contentFingerprint.summarize(afterContent)));
    }

    @Transactional
    public void recordReplyDeleted(User admin, Long replyId, Long feedbackId, String content) {
        record(admin, ActionType.REPLY_DELETE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyDeleted(feedbackId, contentFingerprint.summarize(content)));
    }

    @Transactional
    public void recordAttachmentAccess(User admin, Long attachmentId, Long feedbackId) {
        record(admin, ActionType.ATTACHMENT_ACCESS, TargetType.ATTACHMENT, attachmentId,
                AdminAuditMetadata.attachmentAccessed(feedbackId));
    }

    @Transactional
    public void recordDeliveryReplay(User admin, Long deliveryId, SeatNotificationDeliveryStatus previousStatus,
                                     int attemptsBefore) {
        record(admin, ActionType.DELIVERY_REPLAY, TargetType.DELIVERY, deliveryId,
                AdminAuditMetadata.deliveryReplayed(previousStatus, attemptsBefore));
    }

    public Page<AdminActionLogResponse> getActionLogs(Pageable pageable) {
        Pageable boundedPageable = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return adminActionLogRepository.findAllByOrderByCreatedAtDescIdDesc(boundedPageable)
                .map(this::toResponse);
    }

    private void record(User admin, ActionType actionType, TargetType targetType, Long targetId,
                        AdminAuditMetadata metadata) {
        AdminActionLog actionLog = AdminActionLog.builder()
                .admin(admin)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .metaData(serialize(metadata))
                .build();
        adminActionLogRepository.save(actionLog);
    }

    private String serialize(AdminAuditMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("관리자 감사 로그 메타데이터를 직렬화할 수 없습니다.", exception);
        }
    }

    private AdminActionLogResponse toResponse(AdminActionLog actionLog) {
        return new AdminActionLogResponse(
                actionLog.getId(),
                actionLog.getAdmin().getId(),
                actionLog.getActionType(),
                actionLog.getTargetType(),
                actionLog.getTargetId(),
                deserializeSafely(actionLog.getMetaData()),
                actionLog.getCreatedAt());
    }

    private AdminAuditMetadata deserializeSafely(String serializedMetadata) {
        if (serializedMetadata == null || serializedMetadata.isBlank()) {
            return AdminAuditMetadata.legacySanitized();
        }

        try {
            AdminAuditMetadata metadata = objectMapper.readValue(serializedMetadata, AdminAuditMetadata.class);
            return metadata.isSupported() ? metadata : AdminAuditMetadata.legacySanitized();
        } catch (JsonProcessingException exception) {
            return AdminAuditMetadata.legacySanitized();
        }
    }
}
