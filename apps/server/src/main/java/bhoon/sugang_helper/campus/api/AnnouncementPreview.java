package bhoon.sugang_helper.campus.api;

import java.time.LocalDateTime;

public record AnnouncementPreview(
        Long id,
        String title,
        String previewContent,
        boolean pinned,
        LocalDateTime createdAt) {
}
