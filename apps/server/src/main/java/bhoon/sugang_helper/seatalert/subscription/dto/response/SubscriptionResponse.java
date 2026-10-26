package bhoon.sugang_helper.seatalert.subscription.dto.response;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionResponse {

    private final Long id;

    private final String courseKey;

    private final String courseName;

    private final String professorName;

    private final boolean isActive;

    private final LocalDateTime createdAt;

    public static SubscriptionResponse of(Subscription subscription, String courseName, String professorName) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                courseName,
                professorName,
                subscription.isActive(),
                subscription.getCreatedAt());
    }
}
