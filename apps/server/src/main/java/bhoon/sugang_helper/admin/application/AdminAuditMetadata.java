package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/**
 * 버전이 있는 관리자 감사 로그 메타데이터 envelope입니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminAuditMetadata(String schema, int version, String event, AdminAuditMetadataData data) {

    private static final String SCHEMA = "admin-action";
    private static final int VERSION = 1;
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "STATUS_CHANGED",
            "REPLY_CREATED",
            "REPLY_UPDATED",
            "REPLY_DELETED",
            "ATTACHMENT_ACCESSED",
            "DELIVERY_REPLAYED",
            "LEGACY_SANITIZED");

    public static AdminAuditMetadata statusChanged(FeedbackStatus previousStatus, FeedbackStatus newStatus) {
        return create("STATUS_CHANGED", new AdminAuditMetadataData(
                null, null, null, previousStatus.name(), newStatus.name(), null, null, null));
    }

    public static AdminAuditMetadata replyCreated(Long feedbackId, ContentSummary content) {
        return create("REPLY_CREATED", new AdminAuditMetadataData(
                feedbackId, null, content, null, null, null, null, null));
    }

    public static AdminAuditMetadata replyUpdated(Long feedbackId, ContentSummary beforeContent,
                                                   ContentSummary afterContent) {
        return create("REPLY_UPDATED", new AdminAuditMetadataData(
                feedbackId,
                beforeContent,
                afterContent,
                null,
                null,
                null,
                null,
                null));
    }

    public static AdminAuditMetadata replyDeleted(Long feedbackId, ContentSummary content) {
        return create("REPLY_DELETED", new AdminAuditMetadataData(
                feedbackId, content, null, null, null, null, null, null));
    }

    public static AdminAuditMetadata attachmentAccessed(Long feedbackId) {
        return create("ATTACHMENT_ACCESSED", new AdminAuditMetadataData(
                feedbackId, null, null, null, null, null, null, null));
    }

    public static AdminAuditMetadata deliveryReplayed(SeatNotificationDeliveryStatus previousStatus, int attemptsBefore) {
        return create("DELIVERY_REPLAYED", new AdminAuditMetadataData(
                null, null, null, previousStatus.name(), null, attemptsBefore, true, null));
    }

    public static AdminAuditMetadata legacySanitized() {
        return create("LEGACY_SANITIZED", new AdminAuditMetadataData(
                null, null, null, null, null, null, null, "legacy_metadata_removed"));
    }

    public boolean isSupported() {
        return SCHEMA.equals(schema) && version == VERSION && data != null && SUPPORTED_EVENTS.contains(event);
    }

    private static AdminAuditMetadata create(String event, AdminAuditMetadataData data) {
        return new AdminAuditMetadata(SCHEMA, VERSION, event, data);
    }
}
