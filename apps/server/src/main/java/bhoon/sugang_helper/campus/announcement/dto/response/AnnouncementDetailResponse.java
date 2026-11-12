package bhoon.sugang_helper.campus.announcement.dto.response;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnouncementDetailResponse {

    private final Long id;

    private final String title;

    private final String content;

    private final boolean pinned;

    private final boolean published;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    private final LocalDateTime firstPublishedAt;

    private final LocalDateTime deletedAt;

    public static AnnouncementDetailResponse from(Announcement announcement) {
        return new AnnouncementDetailResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPinned(),
                announcement.isPublished(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                announcement.getFirstPublishedAt(),
                announcement.getDeletedAt());
    }
}
