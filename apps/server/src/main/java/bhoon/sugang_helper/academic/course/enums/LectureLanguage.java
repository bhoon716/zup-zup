package bhoon.sugang_helper.academic.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum LectureLanguage {
    KOREAN("한국어", "한국어"),
    ENGLISH("영어", "English"),
    GERMAN("독일어", "독일어"),
    SPANISH("스페인어", "스페인어"),
    JAPANESE("일본어", "일본어"),
    CHINESE("중국어", "중국어"),
    FRENCH("프랑스어", "프랑스어");

    private final String value;
    private final String alias;

    public static LectureLanguage from(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(language -> language.matches(normalized))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String candidate) {
        return value.equalsIgnoreCase(candidate) || alias.equalsIgnoreCase(candidate);
    }
}
