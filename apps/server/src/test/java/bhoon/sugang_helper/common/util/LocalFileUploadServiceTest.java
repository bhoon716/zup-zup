package bhoon.sugang_helper.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class LocalFileUploadServiceTest {

    @TempDir
    Path uploadDirectory;

    private final LocalFileUploadService fileUploadService = new LocalFileUploadService();

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackDeletesUploadedFiles() throws Exception {
        configureUploadDirectory();
        Path uploadedFile = Files.writeString(uploadDirectory.resolve("rolled-back.png"), "content");
        TransactionSynchronizationManager.initSynchronization();

        fileUploadService.deleteFilesAfterTransactionRollback(List.of("/uploads/rolled-back.png"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(uploadedFile).doesNotExist();
    }

    @Test
    void svgUploadIsRejectedEvenWhenItClaimsAnImageMimeType() {
        configureUploadDirectory();
        MockMultipartFile svg = new MockMultipartFile(
                "file", "script.svg", "image/svg+xml", "<svg><script>alert(1)</script></svg>".getBytes());

        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(svg)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE_TYPE);
    }

    private void configureUploadDirectory() {
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", uploadDirectory.toString());
        ReflectionTestUtils.setField(fileUploadService, "orphanMinimumAge", Duration.ofHours(24));
    }
}
