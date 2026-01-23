package bhoon.sugang_helper.domain.subscription.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscription_course_key", columnList = "courseKey"),
        @Index(name = "idx_subscription_user_id", columnList = "userId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String courseKey;

    @Column(nullable = false)
    private boolean isActive = true;

    @Builder
    public Subscription(Long userId, String courseKey, boolean isActive) {
        this.userId = userId;
        this.courseKey = courseKey;
        this.isActive = isActive;
    }

    public void cancel() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
