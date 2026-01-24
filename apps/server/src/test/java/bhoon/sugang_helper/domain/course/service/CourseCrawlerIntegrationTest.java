package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.domain.notification.sender.EmailNotificationSender;
import bhoon.sugang_helper.domain.notification.sender.FcmNotificationSender;
import bhoon.sugang_helper.domain.notification.sender.NotificationChannel;
import bhoon.sugang_helper.domain.notification.sender.WebPushNotificationSender;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
@Transactional
class CourseCrawlerIntegrationTest {

        @Autowired
        private CourseCrawlerService courseCrawlerService;

        @MockitoBean
        private JbnuCourseApiClient jbnuCourseApiClient;

        @MockitoBean
        private RedisService redisService;

        @MockitoBean
        private EmailNotificationSender emailNotificationSender;

        @MockitoBean
        private FcmNotificationSender fcmNotificationSender;

        @MockitoBean
        private WebPushNotificationSender webPushNotificationSender;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private SubscriptionRepository subscriptionRepository;

        private String beforeXml;
        private String afterXml;

        @BeforeEach
        void setUp() throws IOException {
                beforeXml = StreamUtils.copyToString(
                                new ClassPathResource("mock/course-before.xml").getInputStream(),
                                StandardCharsets.UTF_8);
                afterXml = StreamUtils.copyToString(
                                new ClassPathResource("mock/course-after.xml").getInputStream(),
                                StandardCharsets.UTF_8);

                // 테스트 유저 생성
                User user = userRepository.save(User.builder()
                                .name("Test User")
                                .email("test@example.com")
                                .role(Role.USER)
                                .build());

                // 각 시나리오별 구독 정보 생성
                // 1. 0 -> 0 (STAY_FULL)
                subscriptionRepository.save(Subscription.builder()
                                .userId(user.getId())
                                .courseKey("STAY_FULL-01")
                                .isActive(true)
                                .build());

                // 2. 0 -> 1+ (BECOME_AVAILABLE)
                subscriptionRepository.save(Subscription.builder()
                                .userId(user.getId())
                                .courseKey("BECOME_AVAILABLE-01")
                                .isActive(true)
                                .build());

                // 3. 1+ -> 1+ (STAY_AVAILABLE)
                subscriptionRepository.save(Subscription.builder()
                                .userId(user.getId())
                                .courseKey("STAY_AVAILABLE-01")
                                .isActive(true)
                                .build());

                // Mocking RedisService to avoid deduplication skipping
                given(redisService.hasKey(any())).willReturn(false);

                // Mocking senders
                given(emailNotificationSender.supports(NotificationChannel.EMAIL)).willReturn(true);
                given(fcmNotificationSender.supports(NotificationChannel.FCM)).willReturn(true);
                given(webPushNotificationSender.supports(NotificationChannel.WEB)).willReturn(true);
        }

        @Test
        @DisplayName("잔여석 변화 시나리오 통합 테스트: 0->1+ 인 경우만 알림이 발송된다")
        void expandedVacancyChangeTest() throws IOException {
                // 1. 초기 상태 세팅 (Before XML)
                given(jbnuCourseApiClient.fetchCourseDataXml()).willReturn(beforeXml);
                courseCrawlerService.crawlAndSaveCourses();

                // 초기화 시 알림이 갔을 수 있음 (STAY_AVAILABLE 과목이 처음 등록될 때 등 상황에 따라)
                // 확실한 테스트를 위해 Mock 호출 기록 초기화
                clearInvocations(emailNotificationSender);

                // 2. 상태 변화 반영 (After XML)
                given(jbnuCourseApiClient.fetchCourseDataXml()).willReturn(afterXml);
                courseCrawlerService.crawlAndSaveCourses();

                // 3. 검증
                // BECOME_AVAILABLE-01 에 대해서만 알림이 가야 함 (정확히 1회)
                verify(emailNotificationSender, times(1)).send(
                                any(),
                                argThat(title -> title.contains("여석 발생 과목")),
                                any());

                // STAY_FULL 이나 STAY_AVAILABLE 에 대해서는 알림이 가면 안 됨
                verify(emailNotificationSender, never()).send(
                                any(),
                                argThat(title -> title.contains("항상 만석 과목")),
                                any());
                verify(emailNotificationSender, never()).send(
                                any(),
                                argThat(title -> title.contains("항상 여석 과목")),
                                any());
        }
}
