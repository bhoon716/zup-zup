package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.cache.RedisAccess;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.client.NotificationProviderResilience;
import bhoon.sugang_helper.seatalert.notification.client.NotificationSender;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.InboxChannelDelivery.Status;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import bhoon.sugang_helper.seatalert.notification.repository.InboxChannelDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"app.notification.outbox.max-attempts=2"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({InboxChannelDeliveries.class, InboxChannelDeliveryWorker.class, NotificationService.class,
        NotificationChannelPolicy.class, InboxWebPushNotices.class, InboxChannelDeliveryTest.TestConfig.class})
class InboxChannelDeliveryTest {

    private static final Long USER_ID = 1L;
    private static final Long INBOX_ID = 10L;
    private static final String TITLE = "문의에 답변이 도착했어요";
    private static final String MESSAGE = "받은 알림에서 확인해 주세요.";
    private static final String EMAIL_KEY = "inbox:10:EMAIL";
    private static final NotificationRecipient EMAIL = recipient(true, false);
    private static final NotificationRecipient WEB = recipient(false, true);
    private static final NotificationRecipient DISABLED = recipient(false, false);

    @Autowired private InboxChannelDeliveries deliveries;
    @Autowired private InboxChannelDeliveryWorker worker;
    @Autowired private InboxChannelDeliveryRepository jobs;
    @Autowired private PlatformTransactionManager transactions;

    @MockitoBean private NotificationRecipients recipients;
    @MockitoBean private WebPushSubscriptionManagement subscriptions;
    @MockitoBean private InboxDeliveryNotices notices;
    @MockitoBean private NotificationProviderResilience resilience;
    @MockitoBean private NotificationSender sender;
    @MockitoBean private RedisAccess redis;
    @MockitoBean(name = "notificationTaskScheduler") private TaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        jobs.deleteAll();
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(resilience).execute(any(), any());
        when(sender.supports(any())).thenReturn(true);
    }

    @Test
    @DisplayName("원본 트랜잭션이 롤백되면 외부 발송 작업도 남지 않는다")
    void enqueueJoinsProducerTransaction() {
        // given
        TransactionTemplate transaction = new TransactionTemplate(transactions);

        // when
        transaction.executeWithoutResult(status -> {
            deliveries.enqueue(INBOX_ID, EMAIL, TITLE, MESSAGE);
            status.setRollbackOnly();
        });

        // then
        assertThat(deliveries.claimReady()).isEmpty();
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("채널이 비활성화되거나 원본이 취소된 작업은 발송하지 않는다")
    void pendingJobRechecksPreferencesAndCancellation() {
        // given
        currentUser(DISABLED);
        enqueue(EMAIL);
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                deliveries.enqueue(11L, EMAIL, TITLE, MESSAGE));
        cancel(11L);

        // when
        worker.processReadyDeliveries();

        // then
        verifyNoInteractions(sender, notices);
        assertThat(jobs.findAll()).extracting(InboxChannelDelivery::getStatus).containsOnly(Status.SKIPPED);
    }

    @Test
    @DisplayName("취소와 제공자 오류가 겹치면 이미 취소된 작업에 실패 알림을 남기지 않는다")
    void cancellationDuringProviderAttemptSuppressesFailureNotice() {
        // given
        currentUser(EMAIL);
        enqueue(EMAIL);
        doAnswer(invocation -> {
            cancel(INBOX_ID);
            throw new NotificationProviderException(ErrorCode.EMAIL_SEND_ERROR, false, "PERMANENT", 400, null);
        }).when(sender).sendDelivery(any(), anyString(), anyString(), anyString(), anyString());

        // when
        worker.processReadyDeliveries();

        // then
        assertThat(jobs.findById(EMAIL_KEY)).get().extracting(InboxChannelDelivery::getStatus)
                .isEqualTo(Status.SKIPPED);
        verifyNoInteractions(notices);
    }

    @Test
    @DisplayName("실패 알림은 마지막 재시도 뒤에만 저장하고 정산 재시도 중 다시 발송하지 않는다")
    void onlyFinalProviderFailureCreatesNoticeAndSettlementDoesNotRedispatch() {
        // given
        currentUser(EMAIL);
        enqueue(EMAIL);
        doThrow(new NotificationProviderException(ErrorCode.EMAIL_SEND_ERROR, true, "OUTAGE", 503, null))
                .when(sender).sendDelivery(any(), anyString(), anyString(), anyString(), anyString());

        // when
        worker.processReadyDeliveries();

        // then
        verifyNoInteractions(notices);
        InboxChannelDelivery pending = jobs.findById(EMAIL_KEY).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(Status.PENDING);

        // given
        ReflectionTestUtils.setField(pending, "nextAttemptAt", LocalDateTime.now().minusSeconds(1));
        jobs.saveAndFlush(pending);
        doThrow(new TransientDataAccessResourceException("정산 재시도 검증")).doNothing()
                .when(notices).recordFailure(USER_ID, EMAIL_KEY, null, null, NotificationChannel.EMAIL, null);

        // when
        worker.processReadyDeliveries();

        // then
        verify(sender, times(2)).sendDelivery(any(), eq(TITLE),
                eq(MESSAGE + "\n\nhttps://www.zup-zup.com/notifications"),
                eq(NotificationDeliveryIdempotencyKey.providerKey(EMAIL_KEY)), eq("/notifications"));
        verify(notices, times(2)).recordFailure(USER_ID, EMAIL_KEY, null, null, NotificationChannel.EMAIL, null);
        assertThat(jobs.findById(EMAIL_KEY)).get().extracting(InboxChannelDelivery::getStatus)
                .isEqualTo(Status.FAILED);
    }

    @Test
    @DisplayName("만료된 웹 푸시 기기의 실패는 실제 기기에 귀속하고 다른 기기는 발송한다")
    void expiredWebDeviceDoesNotAffectAnotherDevice() {
        // given
        NotificationDevice expired = new NotificationDevice(20L, USER_ID, "expired", "key1", "auth1", "브라우저 A");
        NotificationDevice active = new NotificationDevice(21L, USER_ID, "active", "key2", "auth2", "브라우저 B");
        currentUser(WEB);
        when(recipients.findDevicesByUserId(USER_ID)).thenReturn(List.of(expired, active));
        enqueue(WEB);
        when(subscriptions.deleteInvalidSubscription(expired)).thenAnswer(invocation -> {
            when(recipients.findDevicesByUserId(USER_ID)).thenReturn(List.of(active));
            return true;
        });
        doAnswer(invocation -> {
            NotificationTarget target = invocation.getArgument(0);
            if (target.getDevice().id().equals(expired.id())) {
                throw new NotificationProviderException(ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION,
                        false, "INVALID_RECIPIENT", 410, null);
            }
            return null;
        }).when(sender).sendDelivery(any(), anyString(), anyString(), anyString(), anyString());

        // when
        worker.processReadyDeliveries();

        // then
        verify(notices).recordFailure(USER_ID, "inbox:10:WEB:20", null, null, NotificationChannel.WEB, expired.alias());
        verify(notices, never()).recordReregistration(any(), anyString(), any(), any());
        assertThat(jobs.findById("inbox:10:WEB:21")).get().extracting(InboxChannelDelivery::getStatus)
                .isEqualTo(Status.SENT);
        verify(sender, times(2)).sendDelivery(any(), eq(TITLE), eq(MESSAGE), anyString(), eq("/notifications"));
    }

    @Test
    @DisplayName("등록된 기기가 없는 웹 푸시 알림은 받은 알림에만 기기 등록을 안내한다")
    void onlyEnabledWebWithoutDevicesCreatesReregistrationNotice() {
        // given
        when(recipients.findDevicesByUserId(USER_ID)).thenReturn(List.of());

        // when
        enqueue(WEB);

        // then
        verify(notices).recordReregistration(USER_ID, "web:missing:inbox:10", null, null);
        assertThat(deliveries.claimReady()).isEmpty();
        verifyNoInteractions(sender);
    }

    private void enqueue(NotificationRecipient user) {
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                deliveries.enqueue(INBOX_ID, user, TITLE, MESSAGE));
    }

    private void cancel(Long inboxId) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> deliveries.cancel(List.of(inboxId)));
    }

    private void currentUser(NotificationRecipient user) {
        when(recipients.findById(USER_ID)).thenReturn(Optional.of(user));
        when(recipients.findActiveByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    }

    private static NotificationRecipient recipient(boolean email, boolean web) {
        return new NotificationRecipient(USER_ID, false, "user@example.com", null, email, web, false, null);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true);
        }
    }
}
