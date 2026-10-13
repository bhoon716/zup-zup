package bhoon.sugang_helper.support.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackAccountLifecycleServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackAccountLifecycleService lifecycleService;

    @Test
    void deleteAllForUserSoftDeletesExistingFeedback() {
        Feedback feedback = Feedback.builder()
                .userId(7L)
                .type(FeedbackType.BUG)
                .title("제목")
                .content("내용")
                .build();
        when(feedbackRepository.findAllByUserId(7L)).thenReturn(List.of(feedback));

        lifecycleService.deleteAllForUser(7L);

        assertThat(feedback.getDeletedAt()).isNotNull();
    }
}
