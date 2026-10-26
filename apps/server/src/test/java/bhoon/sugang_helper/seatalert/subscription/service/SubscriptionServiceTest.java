package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.dto.result.CourseName;
import bhoon.sugang_helper.academic.course.dto.result.CourseSummary;
import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.course.service.CourseCatalogQueryService;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
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

    private static final String USER_EMAIL = "test@example.com";

    private static final String COURSE_NAME = "Test Course";

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private CourseCatalogQueryService courseCatalogQuery;
    @Mock
    private UserIdentityQueryService userIdentityQuery;
    @Mock
    private CourseCrawlerTargetService crawlerTargetQuery;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UserIdentity user;
    private CourseSummary course;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "maxLimit", 3);
        user = new UserIdentity(1L, false);
        course = summary("CK1", COURSE_NAME, "Prof", "2026", "U211600010");
    }

    @Test
    @DisplayName("구독 신청 성공")
    void subscribe_success() {

        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(USER_EMAIL);
            given(userIdentityQuery.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
            given(courseCatalogQuery.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                    .willReturn(Optional.empty());
            given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.id())).willReturn(0L);
            given(crawlerTargetQuery.getCurrentTargetValue())
                    .willReturn(new CrawlTargetInfo("2026", SemesterType.fromCode("U211600010")));

            Subscription subscription = Subscription.builder()
                    .userId(user.id())
                    .courseKey(course.courseKey())
                    .isActive(true)
                    .build();
            ReflectionTestUtils.setField(subscription, "id", 1L);
            given(subscriptionRepository.save(any(Subscription.class))).willReturn(subscription);


            SubscriptionResponse response = subscriptionService.subscribe(request);


            assertThat(response.getCourseKey()).isEqualTo(course.courseKey());
            assertThat(response.getCourseName()).isEqualTo(course.name());
        }
    }

    @Test
    @DisplayName("구독 신청 실패 - 중복 구독")
    void subscribe_alreadyExists_throwsException() {

        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(USER_EMAIL);
            given(userIdentityQuery.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
            given(courseCatalogQuery.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                    .willReturn(Optional.of(Subscription.builder().build()));


            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }
    }

    @Test
    @DisplayName("구독 신청 실패 - 최대 개수 초과")
    void subscribe_limitExceeded_throwsException() {

        SubscriptionRequest request = new SubscriptionRequest("CK1");
        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(USER_EMAIL);
            given(userIdentityQuery.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
            given(courseCatalogQuery.findByCourseKey(request.getCourseKey())).willReturn(Optional.of(course));
            given(subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()))
                    .willReturn(Optional.empty());
            given(subscriptionRepository.countByUserIdAndIsActiveTrue(user.id())).willReturn(3L);


            assertThatThrownBy(() -> subscriptionService.subscribe(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }
    }

    @Test
    @DisplayName("내 구독 목록의 강의는 한 번의 bulk 조회로 결합한다")
    void getMySubscriptions_bulkFetchesCourses() {
        Subscription first = Subscription.builder().userId(user.id()).courseKey("CK1").isActive(true).build();
        Subscription second = Subscription.builder().userId(user.id()).courseKey("CK2").isActive(true).build();
        CourseName secondCourse = new CourseName("CK2", "Second Course", "Second Prof");

        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(USER_EMAIL);
            given(userIdentityQuery.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
            given(subscriptionRepository.findByUserId(user.id())).willReturn(List.of(first, second));
            given(courseCatalogQuery.findNamesByCourseKeyIn(List.of("CK1", "CK2")))
                    .willReturn(List.of(new CourseName("CK1", COURSE_NAME, "Prof"), secondCourse));

            List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();

            assertThat(responses).extracting(SubscriptionResponse::getCourseName)
                    .containsExactly(COURSE_NAME, "Second Course");
            verify(courseCatalogQuery).findNamesByCourseKeyIn(List.of("CK1", "CK2"));
            verify(courseCatalogQuery, never()).findByCourseKey(any());
        }
    }

    @Test
    @DisplayName("내 구독 목록의 중복 courseKey는 기존 강의로 병합한다")
    void getMySubscriptions_duplicateCourseKey_keepsExistingCourse() {
        Subscription subscription = Subscription.builder().userId(user.id()).courseKey("CK1").isActive(true).build();
        CourseName replacementCourse = new CourseName("CK1", "Replacement Course", "Replacement Prof");

        try (var mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserEmail).thenReturn(USER_EMAIL);
            given(userIdentityQuery.findByEmail(USER_EMAIL)).willReturn(Optional.of(user));
            given(subscriptionRepository.findByUserId(user.id())).willReturn(List.of(subscription));
            given(courseCatalogQuery.findNamesByCourseKeyIn(List.of("CK1")))
                    .willReturn(List.of(new CourseName("CK1", COURSE_NAME, "Prof"), replacementCourse));

            List<SubscriptionResponse> responses = subscriptionService.getMySubscriptions();

            assertThat(responses).singleElement().satisfies(response ->
                    assertThat(response.getCourseName()).isEqualTo(COURSE_NAME));
        }
    }

    private CourseSummary summary(String key, String name, String professor, String year, String semester) {
        return new CourseSummary(key, key, "1", name, professor, 30, 0, year, semester,
                null, "3", "", "", List.of());
    }
}
