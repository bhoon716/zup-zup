package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.admin.contract.AdminAuditRecorder;
import bhoon.sugang_helper.admin.domain.ActionType;
import bhoon.sugang_helper.admin.domain.AdminActionLog;
import bhoon.sugang_helper.admin.domain.AdminActionLogRepository;
import bhoon.sugang_helper.admin.domain.TargetType;
import bhoon.sugang_helper.common.web.PageableGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 민감한 관리자 조작을 동일한 최소화된 형식으로 감사합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditService implements AdminAuditRecorder {

    private static final int MAX_PAGE_SIZE = 100;

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

    public Page<AdminActionLogResponse> getActionLogs(Pageable pageable) {
        Pageable requested = PageableGuard.requireBounded(pageable, MAX_PAGE_SIZE, 10_000);
        Pageable boundedPageable = PageRequest.of(requested.getPageNumber(), requested.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return adminActionLogRepository.findAllByOrderByCreatedAtDescIdDesc(boundedPageable)
                .map(this::toResponse);
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

    private AdminActionLogResponse toResponse(AdminActionLog actionLog) {
        return new AdminActionLogResponse(
                actionLog.getId(),
                actionLog.getAdminId(),
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
