package bhoon.sugang_helper.domain.course.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetGrade {
    GRADE_1("1", "1학년"),
    GRADE_2("2", "2학년"),
    GRADE_3("3", "3학년"),
    GRADE_4("4", "4학년"),
    GRADE_5("5", "5학년"),
    GRADE_6("6", "6학년"),
    NONE("없음", "없음");

    private final String code;
    private final String description;

    @JsonCreator
    public static TargetGrade from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return Arrays.stream(values())
                .filter(g -> g.code.equals(value) || g.description.equals(value.trim()))
                .findFirst()
                .orElse(NONE);
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
