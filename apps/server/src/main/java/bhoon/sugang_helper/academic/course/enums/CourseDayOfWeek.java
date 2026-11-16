package bhoon.sugang_helper.academic.course.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum CourseDayOfWeek {
    MONDAY("월", "MO"),
    TUESDAY("화", "TU"),
    WEDNESDAY("수", "WE"),
    THURSDAY("목", "TH"),
    FRIDAY("금", "FR"),
    SATURDAY("토", "SA"),
    SUNDAY("일", "SU");

    private final String description;
    private final String shortCode;

    private static CourseDayOfWeek parse(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(day -> day.matches(normalized))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String value) {
        return name().equalsIgnoreCase(value) || description.equals(value) || shortCode.equalsIgnoreCase(value);
    }

    public static CourseDayOfWeek from(String description) {
        return parse(description);
    }

    @JsonCreator
    public static CourseDayOfWeek fromJson(String value) {
        return parse(value);
    }
}
