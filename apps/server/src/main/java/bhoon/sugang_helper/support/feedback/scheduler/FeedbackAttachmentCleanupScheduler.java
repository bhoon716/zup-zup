package bhoon.sugang_helper.support.feedback.scheduler;

import bhoon.sugang_helper.common.api.storage.FileStorage;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.repository.FeedbackAttachmentRepository;
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
    private final FileStorage fileUploadService;

    @Scheduled(cron = "${file.upload.orphan-cleanup-cron:0 0 4 * * *}")
    public void removeOrphanedFiles() {
        Set<String> referencedFileUrls = feedbackAttachmentRepository.findAll().stream()
                .map(FeedbackAttachment::getFileUrl)
                .collect(Collectors.toUnmodifiableSet());
        fileUploadService.deleteOrphanedFiles(referencedFileUrls);
        logCleanupCompleted(referencedFileUrls.size());
    }

    private void logCleanupCompleted(int referencedFiles) {
        log.atInfo().addKeyValue("event", "feedback_attachment_cleanup_completed")
                .addKeyValue("referencedFiles", referencedFiles)
                .log("[Feedback Attachment] 참조가 없는 첨부 파일 정리 완료.");
    }

}
