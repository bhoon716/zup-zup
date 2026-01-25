package bhoon.sugang_helper.domain.notification.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_histories", indexes = {
        @Index(name = "idx_notif_hist_user_id", columnList = "userId")
})
public class NotificationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String courseKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Builder
    public NotificationHistory(Long userId, String courseKey, String title, String message,
            NotificationChannel channel) {
        this.userId = userId;
        this.courseKey = courseKey;
        this.title = title;
        this.message = message;
        this.channel = channel;
    }
}
