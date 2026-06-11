package bhoon.sugang_helper.domain.course.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CourseClassificationTest {

    @DisplayName("이수구분 설명(description) 문자열로 올바른 CourseClassification 상수를 반환한다")
    @ParameterizedTest
    @CsvSource({
            "계열공통, SERIES_COMMON",
            "교양, GENERAL_EDUCATION",
            "교직, TEACHING_PROFESSION",
            "군사학, MILITARY_SCIENCE",
            "기초필수, BASIC_REQUIRED",
            "선수, PREREQUISITE",
            "일반선택, GENERAL_ELECTIVE",
            "전공, MAJOR",
            "전공선택, MAJOR_ELECTIVE",
            "전공필수, MAJOR_REQUIRED"
    })
    void fromDescription(String description, CourseClassification expected) {
        assertThat(CourseClassification.from(description)).isEqualTo(expected);
    }

    @DisplayName("축약어 또는 대학원 전공 설명이 들어오는 경우 알맞은 상수로 매핑된다")
    @ParameterizedTest
    @CsvSource({
            "교직(대), TEACHING_PROFESSION_GRAD",
            "교직(대학원), TEACHING_PROFESSION_GRAD",
            "전공(대학원), MAJOR",
            "전공(대), MAJOR"
    })
    void fromAbbreviationOrGraduate(String description, CourseClassification expected) {
        assertThat(CourseClassification.from(description)).isEqualTo(expected);
    }

    @DisplayName("유효하지 않은 문자열이 들어오면 null을 반환한다")
    @Test
    void fromInvalidDescription() {
        assertThat(CourseClassification.from(null)).isNull();
        assertThat(CourseClassification.from("")).isNull();
        assertThat(CourseClassification.from("   ")).isNull();
        assertThat(CourseClassification.from("알수없는구분")).isNull();
    }
}
