package bhoon.sugang_helper.support.feedback.service;

import bhoon.sugang_helper.identity.user.service.UserFeedbackCleanup;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackAccountLifecycleService implements UserFeedbackCleanup {

    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void deleteAllForUser(Long userId) {
        feedbackRepository.findAllByUserId(userId).forEach(feedback -> feedback.delete());
    }
}
