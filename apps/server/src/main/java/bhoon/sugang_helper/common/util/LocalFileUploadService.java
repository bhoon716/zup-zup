package bhoon.sugang_helper.common.util;

import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.error.CustomException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 파일 시스템에 파일을 업로드하고 관리하는 서비스 클래스입니다.
 * 파일의 무결성 검증(MIME 타입 및 확장자) 및 보안 처리를 담당합니다.
 */
@Component
@Slf4j
public class LocalFileUploadService {

    @Value("${file.upload.dir:./data/uploads}")
    private String uploadDir;

    private final Tika tika = new Tika();

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
            if (file.isEmpty())
                continue;

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
            return "/uploads/" + savedName;
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
            if (mimeType == null || !mimeType.startsWith("image/")) {
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
}
