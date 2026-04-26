package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import bhoon.sugang_helper.domain.course.repository.DepartmentRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JbnuCourseParserTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private JbnuCourseParser parser;

    @Test
    @DisplayName("XML에서 강좌 및 상세 메타데이터를 정확하게 파싱합니다.")
    void parseCourses_comprehensive() {
        // Given
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">10001</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="SBJTNM">핵심교양과목</Col>
                            <Col id="RPSTPROFNM">진교수</Col>
                            <Col id="SUSTCDNM">컴퓨터공학</Col>
                            <Col id="CPTNFGNM">교양</Col>
                            <Col id="LMTRCNT">40</Col>
                            <Col id="TLSNRCNT">25</Col>
                            <Col id="SCORTRETFGNM">절대평가</Col>
                            <Col id="DAYTMCTNT">월 1-A, 1-B, 수 2-A</Col>
                            <Col id="PNT">3</Col>
                            <Col id="LTLANGFGNM">한국어</Col>
                            <Col id="PUBCYN">Y</Col>
                            <Col id="NOPUBCRESNNM"></Col>
                            <Col id="TM">3</Col>
                            <Col id="FLDFGNM">인문</Col>
                            <Col id="FLDDETAFGNM">일반</Col>
                            <Col id="VLDFGNM">인증</Col>
                            <Col id="OPENLECTFGNM">정상</Col>
                            <Col id="VILROOMNOCTNT">공대 7호관</Col>
                            <Col id="SUBPLANYN">Y</Col>
                            <Col id="FLDCONVINFO">핵심</Col>
                        </Row>
                    </Rows>
                </Root>
                """;
        
        // When
        List<Course> result = parser.parseCourses(xmlData);

        // Then
        assertThat(result).hasSize(1);
        Course course = result.get(0);
        assertThat(course.getName()).isEqualTo("핵심교양과목");
        assertThat(course.getDepartment()).isEqualTo("컴퓨터공학부");
        assertThat(course.getCapacity()).isEqualTo(40);
        assertThat(course.getSchedules()).hasSize(2);
    }

    @Test
    @DisplayName("학과명 끝에 붙은 학년 정보가 올바르게 추출되고 학과명에서 제거됩니다.")
    void normalizeDepartmentName_withGrade() {
        // Given
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Row>
                        <Col id="SUSTCDNM">전자공 2</Col>
                        <Col id="TLSNOBJFGNM">2학년</Col>
                    </Row>
                </Dataset>
                """;

        // When
        List<Course> result = parser.parseCourses(xmlData);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("전자공학부");
        assertThat(result.get(0).getTargetGrade()).isEqualTo(TargetGrade.GRADE_2);
    }

    @Test
    @DisplayName("과목명 뒤에 붙은 불필요한 분반 번호를 제거합니다.")
    void normalizeSubjectName_removesClassNumber() {
        // Given
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Row>
                        <Col id="SBJTNM">수학 001</Col>
                        <Col id="CLSS">01</Col>
                    </Row>
                </Dataset>
                """;

        // When
        List<Course> result = parser.parseCourses(xmlData);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("수학");
    }

    @Test
    @DisplayName("연속된 교시의 강의 시간을 하나의 스케줄로 병합합니다.")
    void parseSchedules_mergesConsecutiveTime() {
        // Given
        String timeContent = "월 1-A, 1-B, 2-A";

        // When
        List<CourseSchedule> result = parser.parseSchedules(timeContent);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo(CourseDayOfWeek.MONDAY);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("복수 학과가 쉼표로 구분된 경우 각각 표준화하여 재결합합니다.")
    void normalizeDepartmentName_multipleDepartments() {
        // Given
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Row>
                        <Col id="SUSTCDNM">회계 3, 경영 4</Col>
                    </Row>
                </Dataset>
                """;

        // When
        List<Course> result = parser.parseCourses(xmlData);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("회계학과, 경영학과");
    }
}
