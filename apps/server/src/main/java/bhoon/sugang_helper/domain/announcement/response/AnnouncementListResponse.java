package bhoon.sugang_helper.domain.announcement.response;

import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공지사항 목록 응답")
public class AnnouncementListResponse {

    private static final int PREVIEW_LENGTH = 120;

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문 미리보기")
    private String previewContent;

    @Schema(description = "상단 고정 여부")
    private boolean pinned;

    @Schema(description = "작성자 이름")
    private String authorName;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    public static AnnouncementListResponse from(Announcement announcement) {
        return AnnouncementListResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .previewContent(createPreview(announcement.getContent()))
                .pinned(announcement.isPinned())
                .authorName(announcement.getAuthorName())
                .createdAt(announcement.getCreatedAt())
                .build();
    }

    private static String createPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ').trim();
        String plainText = stripMarkdownSyntax(normalized);
        if (plainText.length() <= PREVIEW_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, PREVIEW_LENGTH) + "...";
    }

    private static String stripMarkdownSyntax(String value) {
        return value
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1")
                .replaceAll("#{1,6}\\s*", "")
                .replaceAll(">\\s*", "")
                .trim();
    }
}
