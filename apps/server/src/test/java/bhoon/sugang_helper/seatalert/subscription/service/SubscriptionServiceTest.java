package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemester;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemesterQuery;
import bhoon.sugang_helper.academic.course.service.CourseCatalogQueryService;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.seatalert.subscription.dto.request.SubscriptionRequest;
import bhoon.sugang_helper.seatalert.subscription.dto.response.SubscriptionResponse;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final String COURSE_KEY = "CK1";
    private static final String SECOND_COURSE_KEY = "CK2";
    private static final String SECOND_COURSE_NAME = "Second Course";
    private static final String COURSE_NAME = "Test Course";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CourseCatalogQueryService courseCatalogQuery;

    @Mock
    private UserIdentityQueryService userIdentityQuery;

    @Mock
    private TrackedSemesterQuery crawlerTargetQuery;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UserIdentity user;
    private CourseSummary course;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "maxLimit", 3);
        user = new UserIdentity(1L, false);
        course = summary(COURSE_KEY, COURSE_NAME, "Prof", "2026", "U211600010");
    }

    @Test
    @DisplayName("구독 신청 성공")
    void subscribe_success() {
        // given
        SubscriptionRequest request = new SubscriptionRequest(COURSE_KEY);

        given(userIdentityQuery.requireCurrentUser()).willReturn(user);
        given(courseCatalogQuery.findWithoutSchedulesByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
        given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                .willReturn(Optional.empty());
        given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.id())).willReturn(0L);
        given(crawlerTargetQuery.currentTarget())
                .willReturn(new TrackedSemester("2026", "U211600010"));

        Subscription subscription = Subscription.builder()
                .userId(user.id())
                .courseKey(course.courseKey())
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(subscription, "id", 1L);
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);

        // when
        SubscriptionResponse response = subscriptionService.subscribe(request);

        // then
        assertThat(response.getCourseKey()).isEqualTo(course.courseKey());
        assertThat(response.getCourseName()).isEqualTo(course.name());
    }

    @Test
    @DisplayName("구독 신청 실패 - 중복 구독")
    void subscribe_alreadyExists_throwsException() {
        // given
        SubscriptionRequest request = new SubscriptionRequest(COURSE_KEY);

        given(userIdentityQuery.requireCurrentUser()).willReturn(user);
        given(courseCatalogQuery.findWithoutSchedulesByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
        given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                .willReturn(Optional.of(Subscription.builder().build()));

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(request))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("구독 신청 실패 - 최대 개수 초과")
    void subscribe_limitExceeded_throwsException() {
        // given
        SubscriptionRequest request = new SubscriptionRequest(COURSE_KEY);

        given(userIdentityQuery.requireCurrentUser()).willReturn(user);
        given(courseCatalogQuery.findWithoutSchedulesByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
        given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                .willReturn(Optional.empty());
        given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.id())).willReturn(3L);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(request))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("내 구독 목록의 강의는 한 번의 bulk 조회로 결합한다")
    void getMySubscriptions_bulkFetchesCourses() {
        // given
        Subscription first = Subscription.builder().userId(user.id()).courseKey(COURSE_KEY).isActive(true).build();
        Subscription second = Subscription.builder().userId(user.id()).courseKey(SECOND_COURSE_KEY).isActive(true).build();
        CourseName secondCourse = new CourseName(SECOND_COURSE_KEY, SECOND_COURSE_NAME, "Second Prof");

        given(userIdentityQuery.requireCurrentUser()).willReturn(user);
        given(subscriptionRepository.findByUserId(user.id())).willReturn(List.of(first, second));
        given(courseCatalogQuery.findNamesByCourseKeyIn(List.of(COURSE_KEY, SECOND_COURSE_KEY)))
                .willReturn(List.of(new CourseName(COURSE_KEY, COURSE_NAME, "Prof"), secondCourse));

        // when
        List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();

        // then
        assertThat(responses).extracting(SubscriptionResponse::getCourseName)
                .containsExactly(COURSE_NAME, SECOND_COURSE_NAME);
        verify(courseCatalogQuery).findNamesByCourseKeyIn(List.of(COURSE_KEY, SECOND_COURSE_KEY));
        verify(courseCatalogQuery, never()).findWithoutSchedulesByCourseKey(any());
    }

    @Test
    @DisplayName("내 구독 목록의 중복 courseKey는 기존 강의로 병합한다")
    void getMySubscriptions_duplicateCourseKey_keepsExistingCourse() {
        // given
        Subscription subscription = Subscription.builder().userId(user.id()).courseKey(COURSE_KEY).isActive(true).build();
        CourseName replacementCourse = new CourseName(COURSE_KEY, "Replacement Course", "Replacement Prof");

        given(userIdentityQuery.requireCurrentUser()).willReturn(user);
        given(subscriptionRepository.findByUserId(user.id())).willReturn(List.of(subscription));
        given(courseCatalogQuery.findNamesByCourseKeyIn(List.of(COURSE_KEY)))
                .willReturn(List.of(new CourseName(COURSE_KEY, COURSE_NAME, "Prof"), replacementCourse));

        // when
        List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();

        // then
        assertThat(responses).singleElement().satisfies(response ->
                assertThat(response.getCourseName()).isEqualTo(COURSE_NAME));
    }

    private CourseSummary summary(String key, String name, String professor, String year, String semester) {
        return new CourseSummary(key, key, "1", name, professor, 30, 0, year, semester,
                null, "3", "", "", List.of());
    }
}
