package bhoon.sugang_helper.feedback.presentation;

import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import java.time.LocalDate;

public record FeedbackSearchCondition(
        FeedbackStatus status,
        FeedbackType type,
        String keyword,
        LocalDate startDate,
        LocalDate endDate) {
}
