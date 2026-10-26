package bhoon.sugang_helper.seatalert.subscription.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseName;
import bhoon.sugang_helper.academic.course.dto.result.CourseSummary;
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
    private final CourseCatalogQueryService courseCatalogQuery;
    private final UserIdentityQueryService userIdentityQuery;
    private final CourseCrawlerTargetService crawlerTargetQuery;

    @Value("${app.subscription.max-limit:3}")
    private int maxLimit;

    /**
     * 특정 강의에 대한 여석 알림 구독을 신청
     */
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
        log.info("[Subscription] Created: userId={}, courseKey={}", user.id(), course.courseKey());

        return SubscriptionResponse.of(saved, course.name(), course.professor());
    }

    /**
     * 구독 신청 전 중복 확인, 최대 한도 체크, 대상 학기 일치 여부 등을 검증
     */
    private void validateSubscription(UserIdentity user, CourseSummary course) {
        if (subscriptionRepository.findByUserIdAndCourseKey(user.id(), course.courseKey()).isPresent()) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if (subscriptionRepository.countByUserIdAndIsActiveTrue(user.id()) >= maxLimit) {
            throw new CustomException(ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }

        CrawlTargetInfo target = crawlerTargetQuery.getCurrentTargetValue();
        if (!course.isMatchingTarget(target.year(), target.semester().getCode())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 추적 중인 학기의 강의만 구독할 수 있습니다.");
        }
    }

    /**
     * 특정 구독 정보를 삭제하여 알림 수신을 중단
     */
    @Transactional
    public void unsubscribe(Long subscriptionId) {
        UserIdentity user = getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "구독 정보를 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(user.id())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        subscriptionRepository.delete(subscription);
        log.info("[Subscription] Deleted: userId={}, subscriptionId={}", user.id(), subscriptionId);
    }

    /**
     * 현재 로그인한 사용자의 모든 구독 내역을 일괄 삭제
     */
    @Transactional
    public void unsubscribeAll() {
        UserIdentity user = getCurrentUser();
        subscriptionRepository.deleteAllByUserId(user.id());
        log.info("[Subscription] Deleted All: userId={}", user.id());
    }

    /**
     * 현재 로그인한 사용자의 모든 구독 목록을 조회
     */
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

    /**
     * 구독 엔티티를 응답 DTO로 변환하며 강의 정보를 결합
     */
    private SubscriptionResponse convertToResponse(Subscription subscription, CourseName course) {
        if (course == null) {
            return SubscriptionResponse.of(subscription, "Unknown", "Unknown");
        }
        return SubscriptionResponse.of(subscription, course.name(), course.professor());
    }

    /**
     * 보안 컨텍스트에서 현재 로그인한 사용자 엔티티를 획득
     */
    private UserIdentity getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
