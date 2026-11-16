package bhoon.sugang_helper.campus.announcement.dto.response;

import bhoon.sugang_helper.campus.announcement.AnnouncementPreviewText;
import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnouncementListResponse {

    private final Long id;

    private final String title;

    private final String previewContent;

    private final boolean pinned;

    private final LocalDateTime createdAt;

    public static AnnouncementListResponse from(Announcement announcement) {
        return new AnnouncementListResponse(
                announcement.getId(),
                announcement.getTitle(),
                AnnouncementPreviewText.from(announcement.getContent()),
                announcement.isPinned(),
                announcement.getCreatedAt());
    }
}
