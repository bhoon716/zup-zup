package bhoon.sugang_helper.support.feedback.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.repository.FeedbackAttachmentRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackAttachmentCleanupSchedulerTest {

    @Mock
    private FeedbackAttachmentRepository feedbackAttachmentRepository;

    @Mock
    private LocalFileUploadService fileUploadService;

    @InjectMocks
    private FeedbackAttachmentCleanupScheduler scheduler;

    @Test
    @DisplayName("첨부 정리는 저장된 모든 첨부 URL을 기준으로 사용한다")
    void cleanupUsesAllPersistedAttachmentUrlsAsTheReferenceSet() {
        // given
        given(feedbackAttachmentRepository.findAll()).willReturn(List.of(
                FeedbackAttachment.builder().fileUrl("/uploads/kept.png").originalName("kept.png").build()));

        // when
        scheduler.removeOrphanedFiles();

        // then
        verify(fileUploadService).deleteOrphanedFiles(Set.of("/uploads/kept.png"));
    }
}
