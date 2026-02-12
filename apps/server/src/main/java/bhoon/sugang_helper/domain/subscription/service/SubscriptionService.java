package bhoon.sugang_helper.domain.subscription.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.subscription.request.SubscriptionRequest;
import bhoon.sugang_helper.domain.subscription.response.SubscriptionResponse;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import bhoon.sugang_helper.common.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Value("${app.subscription.max-limit:3}")
    private int maxLimit;

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

    private void validateSubscription(User user, Course course) {
        if (subscriptionRepository.findByUserIdAndCourseKey(user.getId(), course.getCourseKey()).isPresent()) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if (subscriptionRepository.countByUserIdAndIsActiveTrue(user.getId()) >= maxLimit) {
            throw new CustomException(ErrorCode.MAX_SUBSCRIPTION_LIMIT_EXCEEDED);
        }
    }

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

    public List<SubscriptionResponse> getMySubscriptions() {
        User user = getCurrentUser();
        return subscriptionRepository.findByUserId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private SubscriptionResponse convertToResponse(Subscription subscription) {
        return courseRepository.findByCourseKey(subscription.getCourseKey())
                .map(course -> SubscriptionResponse.of(subscription, course.getName(), course.getProfessor()))
                .orElseGet(() -> SubscriptionResponse.of(subscription, "Unknown", "Unknown"));
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
