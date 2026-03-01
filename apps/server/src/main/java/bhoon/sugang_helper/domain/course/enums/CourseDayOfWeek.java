package bhoon.sugang_helper.domain.course.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public static CourseDayOfWeek from(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.trim();
        return Arrays.stream(values())
                .filter(d -> d.name().equalsIgnoreCase(normalized)
                        || d.description.equals(normalized)
                        || d.shortCode.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    @JsonCreator
    public static CourseDayOfWeek fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(d -> d.name().equalsIgnoreCase(normalized)
                        || d.description.equals(normalized)
                        || d.shortCode.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}
