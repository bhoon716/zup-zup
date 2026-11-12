package bhoon.sugang_helper.seatalert.notification.entity;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_channel_deliveries")
public class InboxChannelDelivery {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, updatable = false)
    private Long inboxId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private NotificationChannel channel;

    @Column(updatable = false)
    private Long deviceId;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    private LocalDateTime lockedUntil;

    @Column(length = 36)
    private String claimToken;

    private LocalDateTime finishedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public InboxChannelDelivery(Long inboxId, Long userId, NotificationChannel channel, Long deviceId,
                                String title, String message) {
        this.id = "inbox:" + inboxId + ":" + channel + (deviceId == null ? "" : ":" + deviceId);
        this.inboxId = inboxId;
        this.userId = userId;
        this.channel = channel;
        this.deviceId = deviceId;
        this.title = title;
        this.message = message;
        this.status = Status.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public String claim(LocalDateTime until) {
        this.status = Status.PROCESSING;
        this.lockedUntil = until;
        this.claimToken = UUID.randomUUID().toString();
        return claimToken;
    }

    public boolean isClaimedBy(String token) {
        return status == Status.PROCESSING && Objects.equals(claimToken, token);
    }

    public void sent(LocalDateTime now) {
        finish(Status.SENT, now);
    }

    public void skip(LocalDateTime now) {
        finish(Status.SKIPPED, now);
    }

    @SuppressWarnings("PMD.NullAssignment")
    public boolean fail(int maximumAttempts, LocalDateTime now, LocalDateTime retryAt) {
        this.attempts++;
        if (attempts >= maximumAttempts) {
            finish(Status.FAILED, now);
            return true;
        }

        this.status = Status.PENDING;
        this.nextAttemptAt = retryAt;
        this.claimToken = null;
        this.lockedUntil = null;
        return false;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private void finish(Status terminalStatus, LocalDateTime now) {
        this.status = terminalStatus;
        this.finishedAt = now;
        this.claimToken = null;
        this.lockedUntil = null;
    }

    public enum Status {
        PENDING, PROCESSING, SENT, FAILED, SKIPPED
    }
}
