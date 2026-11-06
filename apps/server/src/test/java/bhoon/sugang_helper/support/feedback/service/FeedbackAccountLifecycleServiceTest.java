package bhoon.sugang_helper.support.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("탈퇴 사용자의 기존 피드백을 논리 삭제한다")
    void deleteAllForUserSoftDeletesExistingFeedback() {
        // given
        Feedback feedback = Feedback.builder()
                .userId(7L)
                .type(FeedbackType.BUG)
                .title("제목")
                .content("내용")
                .build();
        when(feedbackRepository.findAllByUserId(7L)).thenReturn(List.of(feedback));

        // when
        lifecycleService.onUserWithdrawn(new UserWithdrawnEvent(7L));

        // then
        assertThat(feedback.getDeletedAt()).isNotNull();
    }
}
