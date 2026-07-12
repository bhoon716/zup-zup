package bhoon.sugang_helper.admin.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 관리자 감사 로그의 허용된 최소 데이터 필드입니다.
 */
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
