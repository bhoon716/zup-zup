package bhoon.sugang_helper.domain.announcement.response;

import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공지사항 상세 응답")
public class AnnouncementDetailResponse {

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "상단 고정 여부")
    private boolean pinned;

    @Schema(description = "공개 여부")
    private boolean published;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
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
