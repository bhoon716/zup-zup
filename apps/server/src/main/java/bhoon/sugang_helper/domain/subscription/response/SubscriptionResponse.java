package bhoon.sugang_helper.domain.subscription.response;

import bhoon.sugang_helper.domain.subscription.entity.Subscription;
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
    private final String professor;
    private final boolean isActive;
    private final LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                null, // 서비스에서 과목명 조회 후 매핑 예정
                null, // 서비스에서 교수명 조회 후 매핑 예정
                subscription.isActive(),
                subscription.getCreatedAt());
    }

    public static SubscriptionResponse of(Subscription subscription, String courseName, String professor) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCourseKey(),
                courseName,
                professor,
                subscription.isActive(),
                subscription.getCreatedAt());
    }
}
