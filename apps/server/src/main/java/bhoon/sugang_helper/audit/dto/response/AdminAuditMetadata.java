package bhoon.sugang_helper.audit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminAuditMetadata(String schema, int version, String event, AdminAuditMetadataData data) {

    private static final String ATTACHMENT_ACCESSED_EVENT = "ATTACHMENT_ACCESSED";
    private static final String DELIVERY_REPLAYED_EVENT = "DELIVERY_REPLAYED";
    private static final String LEGACY_SANITIZED_EVENT = "LEGACY_SANITIZED";
    private static final String REPLY_CREATED_EVENT = "REPLY_CREATED";
    private static final String REPLY_DELETED_EVENT = "REPLY_DELETED";
    private static final String REPLY_UPDATED_EVENT = "REPLY_UPDATED";
    private static final String STATUS_CHANGED_EVENT = "STATUS_CHANGED";

    private static final String SCHEMA = "admin-action";
    private static final int VERSION = 1;
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            STATUS_CHANGED_EVENT,
            REPLY_CREATED_EVENT,
            REPLY_UPDATED_EVENT,
            REPLY_DELETED_EVENT,
            ATTACHMENT_ACCESSED_EVENT,
            DELIVERY_REPLAYED_EVENT,
            LEGACY_SANITIZED_EVENT);

    public static AdminAuditMetadata statusChanged(String previousStatus, String newStatus) {
        return create(STATUS_CHANGED_EVENT, new AdminAuditMetadataData(
                null, null, null, previousStatus, newStatus, null, null, null));
    }

    public static AdminAuditMetadata replyCreated(Long feedbackId, ContentSummary content) {
        return create(REPLY_CREATED_EVENT, new AdminAuditMetadataData(
                feedbackId, null, content, null, null, null, null, null));
    }

    public static AdminAuditMetadata replyUpdated(Long feedbackId, ContentSummary beforeContent,
                                                   ContentSummary afterContent) {
        return create(REPLY_UPDATED_EVENT, new AdminAuditMetadataData(
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
        return create(REPLY_DELETED_EVENT, new AdminAuditMetadataData(
                feedbackId, content, null, null, null, null, null, null));
    }

    public static AdminAuditMetadata attachmentAccessed(Long feedbackId) {
        return create(ATTACHMENT_ACCESSED_EVENT, new AdminAuditMetadataData(
                feedbackId, null, null, null, null, null, null, null));
    }

    public static AdminAuditMetadata deliveryReplayed(String previousStatus, int attemptsBefore) {
        return create(DELIVERY_REPLAYED_EVENT, new AdminAuditMetadataData(
                null, null, null, previousStatus, null, attemptsBefore, true, null));
    }

    public static AdminAuditMetadata legacySanitized() {
        return create(LEGACY_SANITIZED_EVENT, new AdminAuditMetadataData(
                null, null, null, null, null, null, null, "legacy_metadata_removed"));
    }

    public boolean isSupported() {
        return SCHEMA.equals(schema) && version == VERSION && data != null && SUPPORTED_EVENTS.contains(event);
    }

    private static AdminAuditMetadata create(String event, AdminAuditMetadataData data) {
        return new AdminAuditMetadata(SCHEMA, VERSION, event, data);
    }
}
