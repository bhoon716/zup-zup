package bhoon.sugang_helper.domain.feedback.dto.request;

import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackCreateRequest(
                @NotNull(message = "분류를 선택해주세요.") FeedbackType type,

                @NotBlank(message = "제목을 입력해주세요.") String title,

                @NotBlank(message = "내용을 입력해주세요.") String content, String metaInfo) {
}
