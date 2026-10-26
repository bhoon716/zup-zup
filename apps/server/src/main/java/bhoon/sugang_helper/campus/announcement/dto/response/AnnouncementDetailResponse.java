package bhoon.sugang_helper.campus.announcement.dto.response;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementDetailResponse {

    private Long id;

    private String title;

    private String content;

    private boolean pinned;

    private boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static AnnouncementDetailResponse from(Announcement announcement) {
        return AnnouncementDetailResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .pinned(announcement.isPinned())
                .published(announcement.isPublished())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
