package bhoon.sugang_helper.notification.domain;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seat_notification_deliveries", indexes = {
        @Index(name = "idx_seat_notif_delivery_ready", columnList = "status,next_attempt_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_notif_delivery_outbox_user_channel",
                columnNames = {"outbox_id", "user_id", "channel"})
})
public class SeatNotificationDelivery extends BaseTimeEntity {

    private static final Pattern FAILURE_CODE_PATTERN = Pattern.compile("^(?:[A-Z]\\d{3}|UNEXPECTED)$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbox_id", nullable = false)
    private SeatNotificationOutbox outbox;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatNotificationDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Builder
    private SeatNotificationDelivery(SeatNotificationOutbox outbox, Long userId, NotificationChannel channel) {
        this.outbox = outbox;
        this.userId = userId;
        this.channel = channel;
        this.status = SeatNotificationDeliveryStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public void claim(LocalDateTime until) {
        this.status = SeatNotificationDeliveryStatus.PROCESSING;
        this.lockedUntil = until;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void markSent() {
        this.status = SeatNotificationDeliveryStatus.SENT;
        this.lockedUntil = null;
        this.lastError = null;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public boolean markFailure(String failureCode, int maximumAttempts, LocalDateTime retryAt) {
        this.attempts++;
        this.lastError = failureCode != null && FAILURE_CODE_PATTERN.matcher(failureCode).matches()
                ? failureCode
                : "UNEXPECTED";
        this.lockedUntil = null;
        if (this.attempts >= maximumAttempts) {
            this.status = SeatNotificationDeliveryStatus.DLQ;
            return true;
        }
        this.status = SeatNotificationDeliveryStatus.PENDING;
        this.nextAttemptAt = retryAt;
        return false;
    }
}
