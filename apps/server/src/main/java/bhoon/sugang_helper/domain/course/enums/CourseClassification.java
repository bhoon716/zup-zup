package bhoon.sugang_helper.domain.course.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseClassification {
    SERIES_COMMON("계열공통"),
    GENERAL_EDUCATION("교양"),
    TEACHING_PROFESSION_GRAD("교직(대학원)"),
    TEACHING_PROFESSION("교직"),
    MILITARY_SCIENCE("군사학"),
    BASIC_REQUIRED("기초필수"),
    PREREQUISITE("선수"),
    GENERAL_ELECTIVE("일반선택"),
    MAJOR("전공"),
    MAJOR_ELECTIVE("전공선택"),
    MAJOR_REQUIRED("전공필수");

    private final String description;

    public static CourseClassification from(String description) {
        return Arrays.stream(values())
                .filter(c -> c.description.equals(description))
                .findFirst()
                .orElse(null);
    }
}
