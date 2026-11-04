package bhoon.sugang_helper.academic.course.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

class CourseResponseTest {

    @ParameterizedTest
    @EnumSource(DisclosureStatus.class)
    @NullSource
    void searchResponseIncludesActualDisclosure(DisclosureStatus disclosure) {
        // given
        Course course = Course.builder()
                .courseKey("2026:1:CSE101:01")
                .name("공개 여부 확인 강의")
                .academicYear("2026")
                .semester("U211600010")
                .capacity(30)
                .current(10)
                .disclosure(disclosure)
                .build();

        // when
        CourseResponse response = CourseResponse.from(course, "2026", "U211600010", null);
        JsonNode json = new ObjectMapper().valueToTree(response);

        // then
        assertThat(json.get("courseKey").asText()).isEqualTo(course.getCourseKey());
        assertThat(json.get("name").asText()).isEqualTo(course.getName());
        if (disclosure == null) {
            assertThat(json.get("disclosure").isNull()).isTrue();
            return;
        }
        assertThat(json.get("disclosure").asText()).isEqualTo(disclosure.getDescription());
    }
}
