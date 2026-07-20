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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
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

    @Column(name = "idempotency_key", nullable = false, length = 36, updatable = false)
    private String idempotencyKey;

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

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "dead_lettered_at")
    private LocalDateTime deadLetteredAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    private SeatNotificationDelivery(SeatNotificationOutbox outbox, Long userId, NotificationChannel channel) {
        this.outbox = outbox;
        this.userId = userId;
        this.channel = channel;
        this.idempotencyKey = UUID.randomUUID().toString();
        this.status = SeatNotificationDeliveryStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public String claim(LocalDateTime until) {
        this.status = SeatNotificationDeliveryStatus.PROCESSING;
        this.lockedUntil = until;
        this.claimToken = UUID.randomUUID().toString();
        return this.claimToken;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void markSent() {
        this.status = SeatNotificationDeliveryStatus.SENT;
        this.lockedUntil = null;
        this.claimToken = null;
        this.lastError = null;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public boolean markFailure(String failureCode, int maximumAttempts, LocalDateTime failedAt,
                               LocalDateTime retryAt) {
        this.attempts++;
        this.lastError = failureCode != null && FAILURE_CODE_PATTERN.matcher(failureCode).matches()
                ? failureCode
                : "UNEXPECTED";
        this.lockedUntil = null;
        this.claimToken = null;
        if (this.attempts >= maximumAttempts) {
            this.status = SeatNotificationDeliveryStatus.DLQ;
            this.deadLetteredAt = failedAt;
            return true;
        }
        this.status = SeatNotificationDeliveryStatus.PENDING;
        this.nextAttemptAt = retryAt;
        return false;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void replay(LocalDateTime now) {
        this.status = SeatNotificationDeliveryStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.lockedUntil = null;
        this.claimToken = null;
        this.lastError = null;
        this.deadLetteredAt = null;
    }

    public boolean isClaimedBy(String token) {
        return this.status == SeatNotificationDeliveryStatus.PROCESSING
                && Objects.equals(this.claimToken, token);
    }
}
