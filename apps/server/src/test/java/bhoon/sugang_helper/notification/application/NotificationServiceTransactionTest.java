package bhoon.sugang_helper.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import bhoon.sugang_helper.notification.domain.NotificationHistory;
import bhoon.sugang_helper.notification.domain.NotificationHistoryRepository;
import bhoon.sugang_helper.notification.infra.DiscordNotificationSender;
import bhoon.sugang_helper.notification.infra.EmailNotificationSender;
import bhoon.sugang_helper.notification.infra.FcmNotificationSender;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.WebPushNotificationSender;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({NotificationService.class, NotificationChannelPolicy.class, NotificationServiceTransactionTest.TestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationServiceTransactionTest {

    private static final String COURSE_KEY = "12345-01";
    private static final String COURSE_NAME = "Test Course";
    private static final String PROFESSOR = "Test Professor";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private RedisService redisService;
    @MockBean
    private SubscriptionRepository subscriptionRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserDeviceRepository userDeviceRepository;
    @MockBean
    private NotificationHistoryRepository notificationHistoryRepository;
    @MockBean
    private EmailNotificationSender emailNotificationSender;
    @MockBean
    private FcmNotificationSender fcmNotificationSender;
    @MockBean
    private WebPushNotificationSender webPushNotificationSender;
    @MockBean
    private DiscordNotificationSender discordNotificationSender;

    @Test
    @DisplayName("커밋된 트랜잭션만 알림을 발송한다")
    void deliversOnlyAfterCommit() {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);
        Subscription subscription = Subscription.builder().userId(1L).courseKey(COURSE_KEY).isActive(true).build();
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Tester")
                .role(Role.USER)
                .emailEnabled(true)
                .discordEnabled(false)
                .webPushEnabled(false)
                .fcmEnabled(false)
                .build();

        when(redisService.setValuesIfAbsent(eq("ALERT:" + COURSE_KEY), eq("PENDING"), any(Duration.class)))
                .thenReturn(true);
        when(subscriptionRepository.findByCourseKeyAndIsActiveTrue(COURSE_KEY)).thenReturn(List.of(subscription));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
        when(emailNotificationSender.supports(NotificationChannel.EMAIL)).thenReturn(true);
        when(fcmNotificationSender.supports(NotificationChannel.FCM)).thenReturn(false);
        when(webPushNotificationSender.supports(NotificationChannel.WEB)).thenReturn(false);
        when(discordNotificationSender.supports(NotificationChannel.DISCORD)).thenReturn(false);

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        verify(notificationHistoryRepository, timeout(1000)).save(any(NotificationHistory.class));
        verify(redisService, timeout(1000)).setValues(eq("ALERT:" + COURSE_KEY), eq("SENT"), any(Duration.class));
    }

    @Test
    @DisplayName("롤백된 트랜잭션의 알림은 발송하지 않는다")
    void doesNotDeliverAfterRollback() {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, PROFESSOR, 0, 1);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        // then
        verify(notificationHistoryRepository, never()).save(any(NotificationHistory.class));
        verify(redisService, never()).setValuesIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(emailNotificationSender, never()).send(any(), anyString(), anyString());
    }

    @TestConfiguration
    @EnableAsync
    static class TestConfig {

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true, true);
        }

        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
