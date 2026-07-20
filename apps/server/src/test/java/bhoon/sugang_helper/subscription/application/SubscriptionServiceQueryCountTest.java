package bhoon.sugang_helper.subscription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.domain.SubscriptionRepository;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class SubscriptionServiceQueryCountTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        sessionFactory.getStatistics().clear();
    }

    @Test
    void mySubscriptionsUsesAConstantNumberOfQueriesRegardlessOfSubscriptionCount() {
        User user = userRepository.save(User.builder()
                .name("Tester")
                .email("test@example.com")
                .role(Role.USER)
                .build());
        Course first = courseRepository.save(course("course-1", "First"));
        Course second = courseRepository.save(course("course-2", "Second"));
        subscriptionRepository.save(Subscription.builder().userId(user.getId()).courseKey(first.getCourseKey())
                .isActive(true).build());
        subscriptionRepository.save(Subscription.builder().userId(user.getId()).courseKey(second.getCourseKey())
                .isActive(true).build());
        entityManager.flush();
        entityManager.clear();
        sessionFactory.getStatistics().clear();

        SubscriptionService service = new SubscriptionService(subscriptionRepository, courseRepository, userRepository,
                org.mockito.Mockito.mock(CourseCrawlerTargetService.class));
        ReflectionTestUtils.setField(service, "maxLimit", 3);
        try (var securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn("test@example.com");

            assertThat(service.getMySubscriptions()).hasSize(2);
        }

        assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isEqualTo(3);
    }

    private Course course(String courseKey, String name) {
        return Course.builder()
                .courseKey(courseKey)
                .subjectCode(courseKey)
                .name(name)
                .classNumber("1")
                .professor("Professor")
                .capacity(30)
                .current(0)
                .academicYear("2026")
                .semester("U211600010")
                .build();
    }
}
