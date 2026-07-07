package bhoon.sugang_helper.feedback.application;

import jakarta.validation.constraints.NotBlank;

public record FeedbackReplyCreateRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.") String content) {
}
