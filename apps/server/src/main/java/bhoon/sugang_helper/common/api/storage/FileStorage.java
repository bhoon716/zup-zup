package bhoon.sugang_helper.common.api.storage;

import java.util.List;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/** 다른 모듈이 사용하는 FileStorage 계약. */
public interface FileStorage {

    List<String> uploadImages(List<MultipartFile> files);

    LoadedFile loadFile(String fileUrl);

    void deleteFilesAfterTransactionCommit(List<String> fileUrls);

    void deleteFilesAfterTransactionRollback(List<String> fileUrls);

    void deleteOrphanedFiles(Set<String> referencedFileUrls);
}
