package bhoon.sugang_helper.common.api.storage;

import java.util.List;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    List<String> uploadImages(List<MultipartFile> files);

    LoadedFile loadFile(String fileUrl);

    void deleteFilesAfterTransactionCommit(List<String> fileUrls);

    void deleteFilesAfterTransactionRollback(List<String> fileUrls);

    void deleteOrphanedFiles(Set<String> referencedFileUrls);
}
