package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.InboxChannelDeliveries.Claim;
import bhoon.sugang_helper.seatalert.notification.service.InboxChannelDeliveries.Dispatch;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboxChannelDeliveryWorker {

    private static final String INBOX_PATH = "/notifications";
    private static final String INBOX_URL = "https://www.zup-zup.com" + INBOX_PATH;
    private static final Duration POLL_WORK_BUDGET = Duration.ofSeconds(5);

    private final InboxChannelDeliveries deliveries;
    private final NotificationService notifications;

    @Value("${app.notification.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-ms:5000}", scheduler = "notificationTaskScheduler")
    public void processReadyDeliveries() {
        long started = System.nanoTime();
        for (int processed = 0; processed < batchSize && !Thread.currentThread().isInterrupted(); processed++) {
            Optional<Claim> claim = deliveries.claimReady();
            if (claim.isEmpty()) {
                return;
            }
            process(claim.get());
            if (System.nanoTime() - started >= POLL_WORK_BUDGET.toNanos()) {
                return;
            }
        }
    }

    void process(Claim claim) {
        try {
            deliveries.findDispatch(claim).ifPresent(dispatch -> {
                RuntimeException failure = attempt(claim, dispatch);
                NotificationDeliveryRetry.settle(() -> {
                    deliveries.settle(claim, dispatch, failure);
                    return null;
                });
            });
        } catch (RuntimeException exception) {
            log.atError().addKeyValue("event", "inbox_delivery_processing_failed")
                    .addKeyValue("deliveryId", claim.id())
                    .addKeyValue("failureCode", SensitiveDataRedactor.failureCode(exception))
                    .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                    .setCause(SensitiveDataRedactor.safeStackTrace(exception))
                    .log("[Notification Inbox] 발송 작업 처리에 실패했습니다.");
        }
    }

    private RuntimeException attempt(Claim claim, Dispatch dispatch) {
        if (dispatch.target() == null) {
            return null;
        }
        String body = dispatch.channel() == NotificationChannel.WEB
                ? dispatch.message() : dispatch.message() + "\n\n" + INBOX_URL;
        try {
            notifications.sendDelivery(dispatch.channel(), dispatch.target(), dispatch.title(), body,
                    claim.id(), INBOX_PATH);
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }
}
