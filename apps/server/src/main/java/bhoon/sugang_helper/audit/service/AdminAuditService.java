package bhoon.sugang_helper.audit.service;

import bhoon.sugang_helper.audit.api.AuditRecorder;
import bhoon.sugang_helper.audit.dto.response.AdminAuditMetadata;
import bhoon.sugang_helper.audit.entity.AdminActionLog;
import bhoon.sugang_helper.audit.enums.ActionType;
import bhoon.sugang_helper.audit.enums.TargetType;
import bhoon.sugang_helper.audit.repository.AdminActionLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditService implements AuditRecorder {

    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;
    private final AdminAuditContentFingerprint contentFingerprint;

    @Transactional
    public void recordStatusChange(Long adminId, Long feedbackId, String previousStatus, String newStatus) {
        record(adminId, ActionType.STATUS_CHANGE, TargetType.FEEDBACK, feedbackId,
                AdminAuditMetadata.statusChanged(previousStatus, newStatus));
    }

    @Transactional
    public void recordReplyCreated(Long adminId, Long replyId, Long feedbackId, String content) {
        record(adminId, ActionType.REPLY_CREATE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyCreated(feedbackId, contentFingerprint.summarize(content)));
    }

    @Transactional
    public void recordReplyUpdated(Long adminId, Long replyId, Long feedbackId, String beforeContent, String afterContent) {
        record(adminId, ActionType.REPLY_UPDATE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyUpdated(
                        feedbackId,
                        contentFingerprint.summarize(beforeContent),
                        contentFingerprint.summarize(afterContent)));
    }

    @Transactional
    public void recordReplyDeleted(Long adminId, Long replyId, Long feedbackId, String content) {
        record(adminId, ActionType.REPLY_DELETE, TargetType.REPLY, replyId,
                AdminAuditMetadata.replyDeleted(feedbackId, contentFingerprint.summarize(content)));
    }

    @Transactional
    public void recordAttachmentAccess(Long adminId, Long attachmentId, Long feedbackId) {
        record(adminId, ActionType.ATTACHMENT_ACCESS, TargetType.ATTACHMENT, attachmentId,
                AdminAuditMetadata.attachmentAccessed(feedbackId));
    }

    @Transactional
    public void recordDeliveryReplay(Long adminId, Long deliveryId, String previousStatus, int attemptsBefore) {
        record(adminId, ActionType.DELIVERY_REPLAY, TargetType.DELIVERY, deliveryId,
                AdminAuditMetadata.deliveryReplayed(previousStatus, attemptsBefore));
    }

    private void record(Long adminId, ActionType actionType, TargetType targetType, Long targetId,
                        AdminAuditMetadata metadata) {
        AdminActionLog actionLog = AdminActionLog.builder()
                .adminId(adminId)
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
}
