package bhoon.sugang_helper.domain.course.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 강의 대상 학년 정보를 정의하는 열거형
 */
@Getter
@RequiredArgsConstructor
public enum TargetGrade {
    GRADE_1("1학년"),
    GRADE_2("2학년"),
    GRADE_3("3학년"),
    GRADE_4("4학년"),
    GRADE_5("5학년"),
    GRADE_6("6학년"),
    GRADUATE("대학원생");

    private final String description;

    /**
     * 입력된 값(코드, 설명, 또는 이름)으로부터 TargetGrade를 추출
     */
    @JsonCreator
    public static TargetGrade from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmedValue = value.trim();
        return Arrays.stream(values())
                .filter(g -> g.name().equals(trimmedValue) ||
                        g.description.equals(trimmedValue) ||
                        (trimmedValue.length() == 1 && g.name().endsWith("_" + trimmedValue)) ||
                        (g == GRADUATE && "대학원".equals(trimmedValue)))
                .findFirst()
                .orElse(null);
    }

    /**
     * JSON 변환 시 설명(description)을 값으로 사용
     */
    @JsonValue
    public String getDescription() {
        return description;
    }
}
