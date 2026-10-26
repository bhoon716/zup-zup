package bhoon.sugang_helper.support.feedback.dto.criteria;

import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.time.LocalDate;

public record FeedbackSearchCondition(
        FeedbackStatus status,
        FeedbackType type,
        String keyword,
        LocalDate startDate,
        LocalDate endDate) {
}
