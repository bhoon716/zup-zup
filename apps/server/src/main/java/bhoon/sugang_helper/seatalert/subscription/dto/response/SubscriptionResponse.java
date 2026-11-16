package bhoon.sugang_helper.seatalert.subscription.dto.response;

import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionResponse {

    private static final String UNKNOWN_COURSE_FIELD = "알 수 없음";

    private final Long id;

    private final String courseKey;

    private final String courseName;

    private final String professorName;

    private final boolean isActive;

    private final LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription subscription, CourseName course) {
        if (course == null) {
            return from(subscription, UNKNOWN_COURSE_FIELD, UNKNOWN_COURSE_FIELD);
        }
        return from(subscription, course.name(), course.professor());
    }

    public static SubscriptionResponse from(Subscription subscription, String courseName, String professorName) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                courseName,
                professorName,
                subscription.isActive(),
                subscription.getCreatedAt());
    }
}
