package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseAccreditation {
    GENERAL("일반"),
    ENGINEERING("공학"),
    MANAGEMENT("경영"),
    NURSING("간호");

    private final String description;

    public static CourseAccreditation from(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.description.trim().equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
