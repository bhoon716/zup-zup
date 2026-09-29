package bhoon.sugang_helper.support.feedback.application;

import bhoon.sugang_helper.support.feedback.contract.FeedbackAccountLifecycle;
import bhoon.sugang_helper.support.feedback.domain.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackAccountLifecycleService implements FeedbackAccountLifecycle {

    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void deleteAllForUser(Long userId) {
        feedbackRepository.findAllByUserId(userId).forEach(feedback -> feedback.delete());
    }
}
