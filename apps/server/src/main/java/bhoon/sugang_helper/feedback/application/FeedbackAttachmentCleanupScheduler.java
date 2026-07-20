package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackAttachmentCleanupScheduler {

    private final FeedbackAttachmentRepository feedbackAttachmentRepository;
    private final LocalFileUploadService fileUploadService;

    @Scheduled(cron = "${file.upload.orphan-cleanup-cron:0 0 4 * * *}")
    public void removeOrphanedFiles() {
        Set<String> referencedFileUrls = feedbackAttachmentRepository.findAll().stream()
                .map(FeedbackAttachment::getFileUrl)
                .collect(Collectors.toUnmodifiableSet());
        fileUploadService.deleteOrphanedFiles(referencedFileUrls);
        log.info("[Feedback Attachment] Orphan cleanup completed. referencedFiles={}", referencedFileUrls.size());
    }
}
