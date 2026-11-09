package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemester;
import bhoon.sugang_helper.academic.api.catalog.TrackedSemesterQuery;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.api.users.Users;
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

    private final SubscriptionRepository subscriptionRepository;
    private final CourseCatalog courseCatalogQuery;
    private final Users userIdentityQuery;
    private final TrackedSemesterQuery crawlerTargetQuery;

    @Value("${app.subscription.max-limit:3}")
    private int maxLimit;

    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request) {
        UserIdentity user = getCurrentUser();
        CourseSummary course = courseCatalogQuery.findByCourseKey(request.getCourseKey())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "과목을 찾을 수 없습니다."));

        validateSubscription(user, course);

        Subscription subscription = Subscription.builder()
                .userId(user.id())
                .courseKey(course.courseKey())
                .isActive(true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("[Subscription] 구독 등록. userId={}, courseKey={}", user.id(), course.courseKey());

        return SubscriptionResponse.of(saved, course.name(), course.professor());
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
        UserIdentity user = getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "구독 정보를 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(user.id())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        subscriptionRepository.delete(subscription);
        log.info("[Subscription] 구독 취소. userId={}, subscriptionId={}", user.id(), subscriptionId);
    }

    @Transactional
    public void unsubscribeAll() {
        UserIdentity user = getCurrentUser();
        subscriptionRepository.deleteAllByUserId(user.id());
        log.info("[Subscription] 전체 구독 취소. userId={}", user.id());
    }

    public List<SubscriptionResponse> getMySubscriptions() {
        UserIdentity user = getCurrentUser();
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(user.id());
        Map<String, CourseName> coursesByKey = courseCatalogQuery.findNamesByCourseKeyIn(subscriptions.stream()
                        .map(Subscription::getCourseKey)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(CourseName::courseKey, Function.identity(),
                        (existing, replacement) -> existing));

        return subscriptions.stream()
                .map(subscription -> convertToResponse(subscription, coursesByKey.get(subscription.getCourseKey())))
                .collect(Collectors.toList());
    }

    private SubscriptionResponse convertToResponse(Subscription subscription, CourseName course) {
        if (course == null) {
            return SubscriptionResponse.of(subscription, "Unknown", "Unknown");
        }
        return SubscriptionResponse.of(subscription, course.name(), course.professor());
    }

    private UserIdentity getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
