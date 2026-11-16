package bhoon.sugang_helper.campus.announcement;

import org.springframework.util.StringUtils;

public final class AnnouncementPreviewText {

    private static final int PREVIEW_LENGTH = 120;

    private AnnouncementPreviewText() {
    }

    public static String from(String content) {
        if (!StringUtils.hasText(content)) {
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
