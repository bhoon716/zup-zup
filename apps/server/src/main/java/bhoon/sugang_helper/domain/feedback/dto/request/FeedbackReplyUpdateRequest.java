package bhoon.sugang_helper.domain.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackReplyUpdateRequest(
        @NotBlank(message = "수정할 답변 내용을 입력해주세요.") String content) {
}
