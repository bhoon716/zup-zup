package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JbnuCourseParserTest {

        private final JbnuCourseParser parser = new JbnuCourseParser();

        @Test
        @DisplayName("Schedules are parsed correctly from valid time string")
        void parseSchedules_valid() {
                // Given
                // XML format mimics what the parser expects in processRow, but here we test the
                // logic indirectly via full parse or just trusted logic?
                // Since parseSchedules is private, we can't test it directly easily without
                // reflection or testing via public method.
                // Let's rely on the public method parseCourses but we need an XML input.
                // Alternatively, we can assume the parser works if the Entity has the data.

                // Let's verify by mocking XML input that produces a known schedule string.
                String xmlData = """
                                <Dataset id="GRD_COUR001">
                                    <Rows>
                                        <Row>
                                            <Col id="SBJTCD">12345</Col>
                                            <Col id="CLSS">01</Col>
                                            <Col id="SBJTNM">Test Course</Col>
                                            <Col id="RPSTPROFNM">홍길동</Col>
                                            <Col id="TLSNOBJFGNM">전체(학부)</Col>
                                            <Col id="DAYTMCTNT">월 6-A,월 6-B,수 3-A</Col>
                                            <Col id="TM">3</Col>
                                            <Col id="OPENLECTFGNM">일반</Col>
                                            <Col id="VLDFGNM">공학</Col>
                                            <Col id="LESSTMFGNM">50분</Col>
                                            <Col id="SUBPLANYN">Y</Col>
                                            <Col id="PUBCYN">공개</Col>
                                            <Col id="FLDCONVINFO">일반,사회과학</Col>
                                        </Row>
                                    </Rows>
                                </Dataset>
                                """;

                // When
                List<Course> courses = parser.parseCourses(xmlData);

                // Then
                assertThat(courses).hasSize(1);
                Course course = courses.get(0);

                // Verify courseKey
                assertThat(course.getCourseKey()).isEqualTo("12345-Test Course-홍길동");

                // Verify new fields
                assertThat(course.getLectureHours()).isEqualTo(3);
                assertThat(course.getTargetGrade()).isEqualTo("전체(학부)");
                assertThat(course.getGeneralCategoryByYear()).isEqualTo("일반,사회과학");
                assertThat(course.getStatus().getDescription()).isEqualTo("일반");
                assertThat(course.getAccreditation().getDescription()).isEqualTo("공학");
                assertThat(course.getClassDuration()).isEqualTo("50분");
                assertThat(course.getHasSyllabus()).isTrue();
                assertThat(course.getDisclosure().getDescription()).isEqualTo("공개");

                // Verify schedules
                List<CourseSchedule> schedules = course.getSchedules();
                assertThat(schedules).hasSize(3);

                assertThat(schedules.get(0).getDayOfWeek()).isEqualTo(CourseDayOfWeek.MONDAY);
                assertThat(schedules.get(0).getPeriod()).isEqualTo(ClassPeriod.PERIOD_6A);

                assertThat(schedules.get(1).getDayOfWeek()).isEqualTo(CourseDayOfWeek.MONDAY);
                assertThat(schedules.get(1).getPeriod()).isEqualTo(ClassPeriod.PERIOD_6B);

                assertThat(schedules.get(2).getDayOfWeek()).isEqualTo(CourseDayOfWeek.WEDNESDAY);
                assertThat(schedules.get(2).getPeriod()).isEqualTo(ClassPeriod.PERIOD_3A);
        }

        @Test
        @DisplayName("Schedules are empty for null or empty string")
        void parseSchedules_empty() {
                String xmlData = """
                                <Dataset id="GRD_COUR001">
                                    <Rows>
                                        <Row>
                                            <Col id="SBJTCD">12345</Col>
                                            <Col id="CLSS">01</Col>
                                            <Col id="SBJTNM">Test Course</Col>
                                            <Col id="DAYTMCTNT"></Col>
                                        </Row>
                                    </Rows>
                                </Dataset>
                                """;

                List<Course> courses = parser.parseCourses(xmlData);
                assertThat(courses).hasSize(1);
                assertThat(courses.get(0).getSchedules()).isEmpty();
        }
}
