package bhoon.sugang_helper.common.util;

import bhoon.sugang_helper.common.dto.result.SanitizedImage;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;

import bhoon.sugang_helper.common.dto.result.LoadedFile;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;




@Component
@Slf4j
public class LocalFileUploadService {

    private static final String UPLOAD_URL_PREFIX = "/uploads/";
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png");
    private static final Map<String, MediaType> SAFE_PREVIEW_MEDIA_TYPES = Map.of(
            "image/jpeg", MediaType.IMAGE_JPEG,
            "image/png", MediaType.IMAGE_PNG);
    private final Tika tika = new Tika();
    private final ImageUploadSanitizer imageUploadSanitizer;
    @Value("${file.upload.dir:./data/uploads}")
    private String uploadDir;
    @Value("${file.upload.orphan-min-age:PT24H}")
    private Duration orphanMinimumAge;

    public LocalFileUploadService(ImageUploadSanitizer imageUploadSanitizer) {
        this.imageUploadSanitizer = imageUploadSanitizer;
    }




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
            log.error("업로드 디렉터리 생성 실패. path= {}", uploadPath, SensitiveDataRedactor.safeStackTrace(e));
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                imageUploadSanitizer.validateSourceSize(file);
                String mimeType = validateImageFile(file);
                SanitizedImage image = imageUploadSanitizer.sanitize(file, mimeType);
                fileUrls.add(saveSanitizedImage(image, uploadPath));
            }
        } catch (RuntimeException exception) {
            deleteFiles(fileUrls);
            throw exception;
        }

        return fileUrls;
    }




    private String saveSanitizedImage(SanitizedImage image, Path uploadPath) {
        String savedName = UUID.randomUUID() + "." + image.extension();
        Path filePath = uploadPath.resolve(savedName);

        try {
            Files.write(filePath, image.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return UPLOAD_URL_PREFIX + savedName;
        } catch (IOException e) {
            deleteFile(filePath);
            log.error("파일 저장 실패. path= {}", filePath, SensitiveDataRedactor.safeStackTrace(e));
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }




    private String validateImageFile(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            String mimeType = tika.detect(input);
            if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }


            String originalName = file.getOriginalFilename();
            if (originalName != null) {
                String lowerName = originalName.toLowerCase();
                if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")
                        && !lowerName.endsWith(".png")) {
                    throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
                }
            }
            return mimeType;
        } catch (IOException e) {
            log.error("이미지 파일 검증 실패.", SensitiveDataRedactor.safeStackTrace(e));
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    public LoadedFile loadFile(String fileUrl) {
        Path filePath = resolveStoredFile(fileUrl);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new CustomException(ErrorCode.NOT_FOUND);
            }
            return new LoadedFile(resource, safePreviewMediaType(filePath));
        } catch (MalformedURLException e) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private MediaType safePreviewMediaType(Path filePath) {
        try (InputStream input = Files.newInputStream(filePath)) {
            return SAFE_PREVIEW_MEDIA_TYPES.getOrDefault(tika.detect(input), MediaType.APPLICATION_OCTET_STREAM);
        } catch (IOException e) {
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
            log.warn("미사용 업로드 파일 검색 실패. path= {}", uploadPath, SensitiveDataRedactor.safeStackTrace(e));
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
            log.warn("업로드 파일 속성 읽기 실패. path= {}", file, SensitiveDataRedactor.safeStackTrace(e));
            return false;
        }
    }

    private void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("업로드 파일 삭제 실패. path= {}", filePath, SensitiveDataRedactor.safeStackTrace(e));
        }
    }
}
