package bhoon.sugang_helper.feedback.application;

import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import java.util.List;
import java.util.Set;
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
    void cleanupUsesAllPersistedAttachmentUrlsAsTheReferenceSet() {
        given(feedbackAttachmentRepository.findAll()).willReturn(List.of(
                FeedbackAttachment.builder().fileUrl("/uploads/kept.png").originalName("kept.png").build()));

        scheduler.removeOrphanedFiles();

        verify(fileUploadService).deleteOrphanedFiles(Set.of("/uploads/kept.png"));
    }
}
