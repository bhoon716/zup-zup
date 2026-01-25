package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseDayOfWeek {
    MONDAY("월"),
    TUESDAY("화"),
    WEDNESDAY("수"),
    THURSDAY("목"),
    FRIDAY("금"),
    SATURDAY("토"),
    SUNDAY("일");

    private final String description;

    public static CourseDayOfWeek from(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(d -> d.description.equals(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
