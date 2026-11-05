package bhoon.sugang_helper.audit.api;

/** 다른 모듈이 사용하는 AuditRecorder 계약. */
public interface AuditRecorder {

    void recordStatusChange(Long adminId, Long feedbackId, String previousStatus, String newStatus);

    void recordReplyCreated(Long adminId, Long replyId, Long feedbackId, String content);

    void recordReplyUpdated(Long adminId, Long replyId, Long feedbackId, String beforeContent, String afterContent);

    void recordReplyDeleted(Long adminId, Long replyId, Long feedbackId, String content);

    void recordAttachmentAccess(Long adminId, Long attachmentId, Long feedbackId);

    void recordDeliveryReplay(Long adminId, Long deliveryId, String previousStatus, int attemptsBefore);
}
