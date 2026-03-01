package bhoon.sugang_helper.domain.subscription.service;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerTargetService;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
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
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseCrawlerTargetService crawlerTargetService;

    @Value("${app.subscription.max-limit:3}")
    private int maxLimit;

    /**
     * 특정 강의에 대한 여석 알림 구독을 신청
     */
    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request) {
        User user = getCurrentUser();
        Course course = courseRepository.findByCourseKey(request.getCourseKey())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "과목을 찾을 수 없습니다."));

        validateSubscription(user, course);

        Subscription subscription = Subscription.builder()
                .userId(user.getId())
                .courseKey(course.getCourseKey())
                .isActive(true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("[Subscription] Created: userId={}, courseKey={}", user.getId(), course.getCourseKey());

        return SubscriptionResponse.of(saved, course.getName(), course.getProfessor());
    }

    /**
     * 구독 신청 전 중복 확인, 최대 한도 체크, 대상 학기 일치 여부 등을 검증
     */
    private void validateSubscription(User user, Course course) {
        if (subscriptionRepository.findByUserIdAndCourseKey(user.getId(), course.getCourseKey()).isPresent()) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if (subscriptionRepository.countByUserIdAndIsActiveTrue(user.getId()) >= maxLimit) {
            throw new CustomException(ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }

        CourseCrawlerTargetService.CrawlTarget target = crawlerTargetService.getCurrentTargetValue();
        if (!course.isMatchingTarget(target.year(), target.semester())) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 추적 중인 학기의 강의만 구독할 수 있습니다.");
        }
    }

    /**
     * 특정 구독 정보를 삭제하여 알림 수신을 중단
     */
    @Transactional
    public void unsubscribe(Long subscriptionId) {
        User user = getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "구독 정보를 찾을 수 없습니다."));

        if (!subscription.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        subscriptionRepository.delete(subscription);
        log.info("[Subscription] Deleted: userId={}, subscriptionId={}", user.getId(), subscriptionId);
    }

    /**
     * 현재 로그인한 사용자의 모든 구독 목록을 조회
     */
    public List<SubscriptionResponse> getMySubscriptions() {
        User user = getCurrentUser();
        return subscriptionRepository.findByUserId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 구독 엔티티를 응답 DTO로 변환하며 강의 정보를 결합
     */
    private SubscriptionResponse convertToResponse(Subscription subscription) {
        return courseRepository.findByCourseKey(subscription.getCourseKey())
                .map(course -> SubscriptionResponse.of(subscription, course.getName(), course.getProfessor()))
                .orElseGet(() -> SubscriptionResponse.of(subscription, "Unknown", "Unknown"));
    }

    /**
     * 보안 컨텍스트에서 현재 로그인한 사용자 엔티티를 획득
     */
    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
