package bhoon.sugang_helper.support.feedback.application;

import bhoon.sugang_helper.support.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.domain.FeedbackType;
import java.time.LocalDate;

public record FeedbackSearchCondition(
        FeedbackStatus status,
        FeedbackType type,
        String keyword,
        LocalDate startDate,
        LocalDate endDate) {
}
