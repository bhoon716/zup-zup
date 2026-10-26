package bhoon.sugang_helper.academic.course.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;




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




    @JsonValue
    public String getDescription() {
        return description;
    }
}
