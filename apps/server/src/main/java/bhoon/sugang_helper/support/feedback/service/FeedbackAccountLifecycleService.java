package bhoon.sugang_helper.support.feedback.service;

import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackAccountLifecycleService {

    private final FeedbackRepository feedbackRepository;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        feedbackRepository.findAllByUserId(event.userId()).forEach(feedback -> feedback.delete());
    }
}
