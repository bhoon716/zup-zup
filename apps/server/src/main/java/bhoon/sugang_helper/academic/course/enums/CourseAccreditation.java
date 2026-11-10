package bhoon.sugang_helper.academic.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum CourseAccreditation {
    GENERAL("일반"),
    ENGINEERING("공학"),
    MANAGEMENT("경영"),
    NURSING("간호");

    private final String description;

    public static CourseAccreditation from(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.description.trim().equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElse(null);
    }
}
