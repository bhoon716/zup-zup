package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemester;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemesterQuery;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.seatalert.subscription.dto.request.SubscriptionRequest;
import bhoon.sugang_helper.seatalert.subscription.dto.response.SubscriptionResponse;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final String EVENT_KEY = "event";
    private static final String USER_ID_KEY = "userId";

    private final SubscriptionRepository subscriptionRepository;
    private final CourseCatalog courseCatalogQuery;
    private final CurrentUserQuery currentUserQuery;
    private final TrackedSemesterQuery crawlerTargetQuery;

    @Value("${app.subscription.max-limit:3}")
    private int maxLimit;

    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request) {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        CourseSummary course = courseCatalogQuery.findWithoutSchedulesByCourseKey(request.getCourseKey())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "과목을 찾을 수 없습니다."));

        validateSubscription(user, course);

        Subscription subscription = Subscription.builder()
                .userId(user.id())
                .courseKey(course.courseKey())
                .isActive(true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        logSubscriptionCreated(user.id(), course.courseKey());

        return SubscriptionResponse.from(saved, course.name(), course.professor());
    }

    private void validateSubscription(UserIdentity user, CourseSummary course) {
        if (subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()).isPresent()) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if (subscriptionRepository.countByUserIdAndIsActiveTrue(user.id()) >= maxLimit) {
            throw new CustomException(ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }

        TrackedSemester target = crawlerTargetQuery.currentTarget();
        if (!course.isMatchingTarget(target.year(), target.semesterCode())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 추적 중인 학기의 강의만 구독할 수 있습니다.");
        }
    }

    @Transactional
    public void unsubscribe(Long subscriptionId) {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "구독 정보를 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(user.id())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        subscriptionRepository.delete(subscription);
        logSubscriptionCancelled(user.id(), subscriptionId);
    }

    @Transactional
    public void unsubscribeAll() {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        subscriptionRepository.deleteAllByUserId(user.id());
        logSubscriptionsCancelled(user.id());
    }

    public List<SubscriptionResponse> getMySubscriptions() {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(user.id());
        Map<String, CourseName> coursesByKey = courseCatalogQuery.findNamesByCourseKeyIn(subscriptions.stream()
                        .map(Subscription::getCourseKey)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(CourseName::courseKey, Function.identity(),
                        (existing, replacement) -> existing));

        return subscriptions.stream()
                .map(subscription -> SubscriptionResponse.from(subscription, coursesByKey.get(subscription.getCourseKey())))
                .collect(Collectors.toList());
    }

    private void logSubscriptionCreated(Long userId, String courseKey) {
        log.atInfo().addKeyValue(EVENT_KEY, "subscription_created")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("courseKey", courseKey)
                .log("[Subscription] 구독 등록.");
    }

    private void logSubscriptionCancelled(Long userId, Long subscriptionId) {
        log.atInfo().addKeyValue(EVENT_KEY, "subscription_cancelled")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("subscriptionId", subscriptionId)
                .log("[Subscription] 구독 취소.");
    }

    private void logSubscriptionsCancelled(Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "subscriptions_cancelled")
                .addKeyValue(USER_ID_KEY, userId)
                .log("[Subscription] 전체 구독 취소.");
    }

}
