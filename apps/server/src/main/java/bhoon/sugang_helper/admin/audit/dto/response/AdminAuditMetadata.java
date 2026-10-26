package bhoon.sugang_helper.admin.audit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;




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

    public static AdminAuditMetadata statusChanged(String previousStatus, String newStatus) {
        return create("STATUS_CHANGED", new AdminAuditMetadataData(
                null, null, null, previousStatus, newStatus, null, null, null));
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

    public static AdminAuditMetadata deliveryReplayed(String previousStatus, int attemptsBefore) {
        return create("DELIVERY_REPLAYED", new AdminAuditMetadataData(
                null, null, null, previousStatus, null, attemptsBefore, true, null));
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
