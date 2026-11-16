package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery.Status;
import bhoon.sugang_helper.seatalert.notification.repository.InboxChannelDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InboxChannelDeliveries {

    private static final int CANCELLATION_BATCH_SIZE = 1_000;
    private static final List<Status> ACTIVE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);

    private final InboxChannelDeliveryRepository deliveries;
    private final NotificationRecipients recipients;
    private final NotificationChannelPolicy policy;
    private final InboxWebPushNotices notices;
    private final NotificationService notificationService;

    @Value("${app.notification.outbox.lease-seconds:60}")
    private long leaseSeconds;

    @Value("${app.notification.outbox.max-attempts:5}")
    private int maximumAttempts;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Long inboxId, NotificationRecipient user, String title, String message) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            if (!policy.isChannelEnabled(user, channel)) {
                continue;
            }
            List<NotificationTarget> targets = notificationService.resolveDeliveryTargets(user, channel);
            if (channel == NotificationChannel.WEB && targets.isEmpty()) {
                notices.recordMissingDevice(user, "inbox:" + inboxId, null, null);
            }
            for (NotificationTarget target : targets) {
                Long deviceId = target.getDevice() == null ? null : target.getDevice().id();
                deliveries.save(new InboxChannelDelivery(inboxId, user.id(), channel, deviceId, title, message));
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(List<Long> inboxIds) {
        LocalDateTime now = LocalDateTime.now();
        for (int start = 0; start < inboxIds.size(); start += CANCELLATION_BATCH_SIZE) {
            int end = Math.min(start + CANCELLATION_BATCH_SIZE, inboxIds.size());
            deliveries.cancel(inboxIds.subList(start, end), ACTIVE_STATUSES, Status.SKIPPED, now);
        }
    }

    @Transactional
    public Optional<Claim> claimReady() {
        LocalDateTime now = LocalDateTime.now();
        return deliveries.findReadyForUpdate(Status.PENDING, Status.PROCESSING, now, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(delivery -> new Claim(delivery.getId(), delivery.getUserId(),
                        delivery.claim(now.plusSeconds(leaseSeconds))));
    }

    @Transactional(readOnly = true)
    public Optional<Dispatch> findDispatch(Claim claim) {
        return deliveries.findById(claim.id())
                .filter(delivery -> delivery.isClaimedBy(claim.token())
                        && delivery.getLockedUntil().isAfter(LocalDateTime.now()))
                .map(delivery -> new Dispatch(delivery.getChannel(), delivery.getTitle(), delivery.getMessage(),
                        findTarget(delivery).orElse(null)));
    }

    @Transactional
    public void settle(Claim claim, Dispatch dispatch, RuntimeException failure) {
        NotificationRecipient user = recipients.findActiveByIdForUpdate(claim.userId()).orElse(null);
        deliveries.findByIdForUpdate(claim.id())
                .filter(delivery -> delivery.isClaimedBy(claim.token()))
                .ifPresent(delivery -> settleDelivery(delivery, dispatch, failure, user));
    }

    private void settleDelivery(InboxChannelDelivery delivery, Dispatch dispatch, RuntimeException failure,
                                NotificationRecipient user) {
        LocalDateTime now = LocalDateTime.now();
        if (dispatch.target() != null && failure == null) {
            delivery.sent(now);
            return;
        }
        if (user == null || !policy.isChannelEnabled(user, delivery.getChannel())) {
            delivery.skip(now);
            return;
        }
        if (dispatch.target() == null) {
            delivery.skip(now);
            if (delivery.getChannel() == NotificationChannel.WEB && !notices.hasUsableDevices(user)) {
                notices.recordMissingDevice(user, "inbox:" + delivery.getInboxId(), null, null);
            }
            return;
        }
        int attempts = NotificationFailureClassifier.isRetryable(failure) ? maximumAttempts : 1;
        boolean terminal = delivery.fail(attempts, now,
                now.plusSeconds(NotificationDeliveryRetry.backoffSeconds(delivery.getAttempts() + 1)));
        notices.recordFailure(user, delivery.getId(), null, null, delivery.getChannel(),
                dispatch.target(), failure, terminal);
    }

    private Optional<NotificationTarget> findTarget(InboxChannelDelivery delivery) {
        return recipients.findById(delivery.getUserId())
                .filter(user -> policy.isChannelEnabled(user, delivery.getChannel()))
                .flatMap(user -> notificationService.resolveDeliveryTargets(user, delivery.getChannel()).stream()
                        .filter(target -> matchesTarget(target, delivery.getDeviceId()))
                        .findFirst());
    }

    private boolean matchesTarget(NotificationTarget target, Long deviceId) {
        NotificationDevice device = target.getDevice();
        return device == null ? deviceId == null : Objects.equals(device.id(), deviceId);
    }

    public record Claim(String id, Long userId, String token) {
    }

    public record Dispatch(NotificationChannel channel, String title, String message, NotificationTarget target) {
    }
}
