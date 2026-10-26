package bhoon.sugang_helper.campus.announcement.dto.response;

import java.time.LocalDateTime;

public record AnnouncementPreview(
        Long id,
        String title,
        String previewContent,
        boolean pinned,
        LocalDateTime createdAt) {
}
