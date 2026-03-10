package bhoon.sugang_helper.domain.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackReplyCreateRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.") String content) {
}
