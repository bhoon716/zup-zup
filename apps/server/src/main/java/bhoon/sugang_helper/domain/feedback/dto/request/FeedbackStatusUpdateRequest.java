package bhoon.sugang_helper.domain.feedback.dto.request;

import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

public record FeedbackStatusUpdateRequest(
        @NotNull(message = "변경할 상태를 선택해주세요.") FeedbackStatus status) {
}
