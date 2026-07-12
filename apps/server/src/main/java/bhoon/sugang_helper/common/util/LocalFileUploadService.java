package bhoon.sugang_helper.common.util;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 파일 시스템에 파일을 업로드하고 관리하는 서비스 클래스입니다. 파일의 무결성 검증(MIME 타입 및 확장자) 및 보안 처리를 담당합니다.
 */
@Component
@Slf4j
public class LocalFileUploadService {

    private static final String UPLOAD_URL_PREFIX = "/uploads/";
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/gif", "image/jpeg", "image/png", "image/webp");
    private final Tika tika = new Tika();
    @Value("${file.upload.dir:./data/uploads}")
    private String uploadDir;
    @Value("${file.upload.orphan-min-age:PT24H}")
    private Duration orphanMinimumAge;

    /**
     * 여러 개의 이미지 파일을 업로드하고 저장된 URL 리스트를 반환합니다.
     */
    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > 3) {
            throw new CustomException(ErrorCode.MAX_FILE_UPLOAD_LIMIT_EXCEEDED);
        }

        List<String> fileUrls = new ArrayList<>();
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directories: {}", uploadPath, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            validateImageFile(file);
            String savedUrl = saveFileToLocal(file, uploadPath);
            fileUrls.add(savedUrl);
        }

        return fileUrls;
    }

    /**
     * 검증된 단일 이미지 파일을 서버 로컬 디렉토리에 저장하고 접근 URL을 반환합니다.
     */
    private String saveFileToLocal(MultipartFile file, Path uploadPath) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String savedName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(savedName);

        try {
            file.transferTo(filePath);
            return UPLOAD_URL_PREFIX + savedName;
        } catch (IOException | IllegalStateException e) {
            log.error("Failed to transfer file to {}", filePath, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 이미지 파일 여부를 MIME 타입과 확장자로 검증합니다.
     */
    private void validateImageFile(MultipartFile file) {
        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }

            // 확장자 기반의 추가 검증 (화이트리스트 방식)
            String originalName = file.getOriginalFilename();
            if (originalName != null) {
                String lowerName = originalName.toLowerCase();
                if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")
                        && !lowerName.endsWith(".png") && !lowerName.endsWith(".webp") && !lowerName.endsWith(".gif")) {
                    throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
                }
            }
        } catch (IOException e) {
            log.error("Failed to validate image file", e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    public Resource loadFile(String fileUrl) {
        Path filePath = resolveStoredFile(fileUrl);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new CustomException(ErrorCode.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    public void deleteFilesAfterTransactionCommit(List<String> fileUrls) {
        if (fileUrls.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteFiles(fileUrls);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFiles(fileUrls);
            }
        });
    }

    public void deleteFilesAfterTransactionRollback(List<String> fileUrls) {
        if (!fileUrls.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteFiles(fileUrls);
                    }
                }
            });
        }
    }

    public void deleteOrphanedFiles(Set<String> referencedFileUrls) {
        Path uploadPath = uploadPath();
        if (!Files.isDirectory(uploadPath)) {
            return;
        }
        try (Stream<Path> files = Files.list(uploadPath)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> !referencedFileUrls.contains(UPLOAD_URL_PREFIX + file.getFileName()))
                    .filter(this::isOlderThanMinimumAge)
                    .forEach(this::deleteFile);
        } catch (IOException e) {
            log.warn("Failed to scan upload directory for orphaned files: {}", uploadPath, e);
        }
    }

    private void deleteFiles(List<String> fileUrls) {
        fileUrls.stream().map(this::resolveStoredFile).forEach(this::deleteFile);
    }

    private Path resolveStoredFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(UPLOAD_URL_PREFIX)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        String filename = fileUrl.substring(UPLOAD_URL_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        Path filePath = uploadPath().resolve(filename).normalize();
        if (!filePath.startsWith(uploadPath())) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return filePath;
    }

    private Path uploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private boolean isOlderThanMinimumAge(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant().isBefore(Instant.now().minus(orphanMinimumAge));
        } catch (IOException e) {
            log.warn("Failed to read upload file metadata: {}", file, e);
            return false;
        }
    }

    private void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete upload file: {}", filePath, e);
        }
    }
}
