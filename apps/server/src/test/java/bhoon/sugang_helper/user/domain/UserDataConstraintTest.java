package bhoon.sugang_helper.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.review.domain.CourseReview;
import bhoon.sugang_helper.review.infra.CourseReviewJpaRepository;
import bhoon.sugang_helper.subscription.domain.Subscription;
import bhoon.sugang_helper.subscription.infra.SubscriptionJpaRepository;
import bhoon.sugang_helper.timetable.domain.Timetable;
import bhoon.sugang_helper.timetable.infra.TimetableJpaRepository;
import bhoon.sugang_helper.user.infra.UserDeviceJpaRepository;
import bhoon.sugang_helper.user.infra.UserJpaRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Constraint scenarios share the same course fixture key.
class UserDataConstraintTest {

    @Autowired
    private SubscriptionJpaRepository subscriptionRepository;

    @Autowired
    private UserDeviceJpaRepository userDeviceRepository;

    @Autowired
    private CourseReviewJpaRepository courseReviewRepository;

    @Autowired
    private TimetableJpaRepository timetableRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void addPrimaryTimetableConstraint() {
        jdbcTemplate.execute("""
                ALTER TABLE timetables
                    ADD COLUMN primary_user_id BIGINT AS (
                        CASE
                            WHEN is_primary THEN user_id
                            ELSE NULL
                        END
                    )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE timetables
                    ADD CONSTRAINT uk_timetable_primary_user UNIQUE (primary_user_id)
                """);
    }

    @Test
    @DisplayName("구독은 사용자와 강의 조합 기준으로 중복 저장을 막는다")
    void subscriptionDuplicateConstraint() {
        subscriptionRepository.saveAndFlush(Subscription.builder()
                .userId(1L)
                .courseKey("COURSE-1")
                .isActive(true)
                .build());

        assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(Subscription.builder()
                .userId(1L)
                .courseKey("COURSE-1")
                .isActive(true)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("기기 토큰은 중복 저장을 막는다")
    void userDeviceDuplicateConstraint() {
        userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(1L)
                .type(DeviceType.WEB)
                .token("token-1")
                .alias("desktop")
                .build());

        assertThatThrownBy(() -> userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(2L)
                .type(DeviceType.WEB)
                .token("token-1")
                .alias("desktop-2")
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("리뷰는 사용자와 강의 조합 기준으로 중복 저장을 막는다")
    void courseReviewDuplicateConstraint() {
        courseReviewRepository.saveAndFlush(CourseReview.builder()
                .courseKey("COURSE-1")
                .userId(1L)
                .rating(5)
                .build());

        assertThatThrownBy(() -> courseReviewRepository.saveAndFlush(CourseReview.builder()
                .courseKey("COURSE-1")
                .userId(1L)
                .rating(4)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사용자 대표 시간표는 하나만 저장할 수 있다")
    void primaryTimetableDuplicateConstraint() {
        timetableRepository.saveAndFlush(Timetable.builder()
                .userId(1L)
                .name("대표 1")
                .isPrimary(true)
                .build());

        assertThatThrownBy(() -> timetableRepository.saveAndFlush(Timetable.builder()
                .userId(1L)
                .name("대표 2")
                .isPrimary(true)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("soft-deleted 사용자는 ID와 이메일 기반 활성 인증 조회에서 제외된다")
    void activeIdentityQueryExcludesSoftDeletedUser() {
        String email = "withdrawn@example.com";
        User user = userRepository.saveAndFlush(User.builder()
                .email(email)
                .name("탈퇴 예정 사용자")
                .role(Role.USER)
                .build());

        assertThat(userRepository.findByIdAndEmailAndDeletedAtIsNull(user.getId(), email)).contains(user);

        user.withdraw();
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByIdAndEmailAndDeletedAtIsNull(user.getId(), email)).isEmpty();
        assertThat(userRepository.existsByIdAndEmailAndDeletedAtIsNull(user.getId(), email)).isFalse();
    }
}
