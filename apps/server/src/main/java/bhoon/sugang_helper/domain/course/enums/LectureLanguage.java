package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(lang -> lang.getValue().equalsIgnoreCase(value.trim()) ||
                        lang.getAlias().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
