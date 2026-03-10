package bhoon.sugang_helper.domain.feedback.dto.request;

import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackType;
import java.time.LocalDate;

public record FeedbackSearchCondition(
        FeedbackStatus status,
        FeedbackType type,
        String keyword,
        LocalDate startDate,
        LocalDate endDate) {
}
