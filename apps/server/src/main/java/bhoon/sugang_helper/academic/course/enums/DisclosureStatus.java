package bhoon.sugang_helper.academic.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum DisclosureStatus {
    PUBLIC("공개"),
    PRIVATE("비공개");

    private final String description;

    public static DisclosureStatus from(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(d -> d.description.equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
