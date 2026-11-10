package bhoon.sugang_helper.campus.announcement.dto.response;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnouncementListResponse {

    private static final int PREVIEW_LENGTH = 120;

    private final Long id;

    private final String title;

    private final String previewContent;

    private final boolean pinned;

    private final LocalDateTime createdAt;

    public static AnnouncementListResponse from(Announcement announcement) {
        return new AnnouncementListResponse(
                announcement.getId(),
                announcement.getTitle(),
                createPreview(announcement.getContent()),
                announcement.isPinned(),
                announcement.getCreatedAt());
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
