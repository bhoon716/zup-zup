package bhoon.sugang_helper.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.UserDeviceRepository;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(classes = NotificationServiceTransactionTest.TestConfig.class)
class NotificationServiceTransactionTest {

    private static final String COURSE_KEY = "12345-01";
    private static final String COURSE_NAME = "Test Course";
    private static final String PROFESSOR = "Test Professor";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RedisService redisService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDeviceRepository userDeviceRepository;
    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;
    @Autowired
    private EmailNotificationSender emailNotificationSender;
    @Autowired
    private FcmNotificationSender fcmNotificationSender;
    @Autowired
    private WebPushNotificationSender webPushNotificationSender;
    @Autowired
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
        when(userDeviceRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of());
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

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    @Import({NotificationService.class, NotificationChannelPolicy.class})
    static class TestConfig {

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(true, true, true, true);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new ResourcelessTransactionManager();
        }

        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        RedisService redisService() {
            return mock(RedisService.class);
        }

        @Bean
        SubscriptionRepository subscriptionRepository() {
            return mock(SubscriptionRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        UserDeviceRepository userDeviceRepository() {
            return mock(UserDeviceRepository.class);
        }

        @Bean
        NotificationHistoryRepository notificationHistoryRepository() {
            return mock(NotificationHistoryRepository.class);
        }

        @Bean
        EmailNotificationSender emailNotificationSender() {
            return mock(EmailNotificationSender.class);
        }

        @Bean
        FcmNotificationSender fcmNotificationSender() {
            return mock(FcmNotificationSender.class);
        }

        @Bean
        WebPushNotificationSender webPushNotificationSender() {
            return mock(WebPushNotificationSender.class);
        }

        @Bean
        DiscordNotificationSender discordNotificationSender() {
            return mock(DiscordNotificationSender.class);
        }
    }
}
