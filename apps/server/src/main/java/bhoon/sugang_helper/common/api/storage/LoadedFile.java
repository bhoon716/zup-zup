package bhoon.sugang_helper.common.api.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record LoadedFile(Resource resource, MediaType contentType) {
}
