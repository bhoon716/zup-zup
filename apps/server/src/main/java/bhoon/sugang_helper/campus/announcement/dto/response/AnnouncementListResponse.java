package bhoon.sugang_helper.campus.announcement.dto.response;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementListResponse {

    private static final int PREVIEW_LENGTH = 120;

    private Long id;

    private String title;

    private String previewContent;

    private boolean pinned;

    private LocalDateTime createdAt;

    public static AnnouncementListResponse from(Announcement announcement) {
        return AnnouncementListResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .previewContent(createPreview(announcement.getContent()))
                .pinned(announcement.isPinned())
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
