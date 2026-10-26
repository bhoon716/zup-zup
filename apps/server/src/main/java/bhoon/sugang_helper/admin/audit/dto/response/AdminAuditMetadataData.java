package bhoon.sugang_helper.admin.audit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;




@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminAuditMetadataData(
        Long feedbackId,
        ContentSummary beforeContent,
        ContentSummary afterContent,
        String previousStatus,
        String newStatus,
        Integer attemptsBefore,
        Boolean idempotencyKeyRetained,
        String reason) {
}
