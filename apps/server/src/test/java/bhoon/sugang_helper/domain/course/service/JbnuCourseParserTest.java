package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JbnuCourseParserTest {

    private final JbnuCourseParser parser = new JbnuCourseParser();

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
                            <Col id="TLSNOBJFGNM">전체(학부)</Col>
                            <Col id="CPTNFGNM">교양</Col>
                            <Col id="SUSTCDNM">교양교육원</Col>
                            <Col id="SCORTRETFGNM">상대평가Ⅰ</Col>
                            <Col id="LTLANGFGNM">한국어</Col>
                            <Col id="DAYTMCTNT">월 1-A,월 1-B,수 1-A</Col>
                            <Col id="PNT">3</Col>
                            <Col id="TM">3</Col>
                            <Col id="LMTRCNT">40</Col>
                            <Col id="TLSNRCNT">10</Col>
                            <Col id="FLDFGNM">균형교양</Col>
                            <Col id="FLDDETAFGNM">삶과사회</Col>
                            <Col id="FLDCONVINFO">핵심,사회이해의기반</Col>
                            <Col id="VLDFGNM">일반</Col>
                            <Col id="OPENLECTFGNM">일반</Col>
                            <Col id="VILROOMNOCTNT">전주:인문대 101</Col>
                            <Col id="SUBPLANYN">Y</Col>
                            <Col id="PUBCYN">공개</Col>
                        </Row>
                        <Row>
                            <Col id="SBJTCD">20002</Col>
                            <Col id="CLSS">02</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="SBJTNM">전공선택과목</Col>
                            <Col id="RPSTPROFNM">이전공</Col>
                            <Col id="TLSNOBJFGNM">3학년</Col>
                            <Col id="CPTNFGNM">전공선택</Col>
                            <Col id="SUSTCDNM">컴퓨터공학부</Col>
                            <Col id="SCORTRETFGNM">절대평가</Col>
                            <Col id="LTLANGFGNM">English</Col>
                            <Col id="DAYTMCTNT">화 3-A,화 3-B,목 3-A,목 3-B</Col>
                            <Col id="PNT">3</Col>
                            <Col id="TM">4</Col>
                            <Col id="LMTRCNT">30</Col>
                            <Col id="TLSNRCNT">30</Col>
                            <Col id="FLDCONVINFO"></Col>
                            <Col id="VLDFGNM">공학</Col>
                            <Col id="OPENLECTFGNM">일반</Col>
                            <Col id="VILROOMNOCTNT">전주:공대 7호관 301</Col>
                            <Col id="SUBPLANYN">N</Col>
                            <Col id="PUBCYN">비공개</Col>
                            <Col id="NOPUBCRESNNM">학과요청</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        // When
        List<Course> courses = parser.parseCourses(xmlData);

        // Then
        assertThat(courses).hasSize(2);

        // 1. 핵심교양과목 검증
        Course course1 = courses.stream().filter(c -> c.getSubjectCode().equals("10001")).findFirst().orElseThrow();
        assertThat(course1.getName()).isEqualTo("핵심교양과목");
        assertThat(course1.getProfessor()).isEqualTo("진교수");
        assertThat(course1.getCourseKey()).isEqualTo("2026:10:10001:01");
        assertThat(course1.getGeneralCategory()).isEqualTo("핵심"); // Prioritized from FLDCONVINFO
        assertThat(course1.getGeneralDetail()).isEqualTo("사회이해의기반");
        assertThat(course1.getClassification().getDescription()).isEqualTo("교양");
        assertThat(course1.getCapacity()).isEqualTo(40);
        assertThat(course1.getCurrent()).isEqualTo(10);
        assertThat(course1.getAvailable()).isEqualTo(30);
        assertThat(course1.getHasSyllabus()).isTrue();
        assertThat(course1.getSchedules()).hasSize(2);
        assertThat(course1.getSchedules().get(0).getDayOfWeek()).isEqualTo(CourseDayOfWeek.MONDAY);
        assertThat(course1.getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(course1.getSchedules().get(0).getEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(course1.getSchedules().get(1).getDayOfWeek()).isEqualTo(CourseDayOfWeek.WEDNESDAY);
        assertThat(course1.getSchedules().get(1).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(course1.getSchedules().get(1).getEndTime()).isEqualTo(LocalTime.of(9, 30));

        // 2. 전공선택과목 검증
        Course course2 = courses.stream().filter(c -> c.getSubjectCode().equals("20002")).findFirst().orElseThrow();
        assertThat(course2.getName()).isEqualTo("전공선택과목");
        assertThat(course2.getClassification().getDescription()).isEqualTo("전공선택");
        assertThat(course2.getDepartment()).isEqualTo("컴퓨터공학부");
        assertThat(course2.getLectureHours()).isEqualTo(4);
        assertThat(course2.getLectureLanguage().getValue()).isEqualTo("영어");
        assertThat(course2.getDisclosure().getDescription()).isEqualTo("비공개");
        assertThat(course2.getDisclosureReason()).isEqualTo("학과요청");
        assertThat(course2.getAccreditation().getDescription()).isEqualTo("공학");
        assertThat(course2.getGeneralCategory()).isNull();

        assertThat(course2.getSchedules()).hasSize(2);
        assertThat(course2.getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(course2.getSchedules().get(0).getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(course2.getSchedules().get(1).getStartTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(course2.getSchedules().get(1).getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("연속된 반교시(A/B)는 하나의 시간 구간으로 병합된다")
    void parseCourses_mergeConsecutiveHalfSlots() {
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">30003</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="DAYTMCTNT">월 1-A, 월 1-B, 수 1-A</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        List<Course> courses = parser.parseCourses(xmlData);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getSchedules()).hasSize(2);
        assertThat(courses.get(0).getSchedules().get(0).getDayOfWeek()).isEqualTo(CourseDayOfWeek.MONDAY);
        assertThat(courses.get(0).getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(courses.get(0).getSchedules().get(0).getEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(courses.get(0).getSchedules().get(1).getDayOfWeek()).isEqualTo(CourseDayOfWeek.WEDNESDAY);
        assertThat(courses.get(0).getSchedules().get(1).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(courses.get(0).getSchedules().get(1).getEndTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("짝이 없는 반교시(B)도 실제 시간 범위로 그대로 보존된다")
    void parseCourses_preserveSingleHalfSlot() {
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">40004</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="DAYTMCTNT">화 3-B</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        List<Course> courses = parser.parseCourses(xmlData);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getSchedules()).hasSize(1);
        assertThat(courses.get(0).getSchedules().get(0).getDayOfWeek()).isEqualTo(CourseDayOfWeek.TUESDAY);
        assertThat(courses.get(0).getSchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(courses.get(0).getSchedules().get(0).getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("TLSNOBJFGNM이 전체(학부)인 경우 SUSTCDNM에서 학년을 추출한다")
    void parseCourses_extractGradeFromSustcdnm() {
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">50005</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="TLSNOBJFGNM">전체(학부)</Col>
                            <Col id="SUSTCDNM">영어영문 3</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        List<Course> courses = parser.parseCourses(xmlData);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getTargetGrade().getDescription()).isEqualTo("3학년");
    }

    @Test
    @DisplayName("SUSTCDNM에 여러 숫자가 있는 경우 마지막 숫자를 학년으로 추출한다 (계열 제외)")
    void parseCourses_extractLastGradeFromSustcdnm() {
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">60006</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="TLSNOBJFGNM">전체(학부)</Col>
                            <Col id="SUSTCDNM">기계시스템 3,기계시스템(정밀기계) 3</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        List<Course> courses = parser.parseCourses(xmlData);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getTargetGrade().getDescription()).isEqualTo("3학년");
    }

    @Test
    @DisplayName("SUSTCDNM에 '계열'이 포함된 경우 숫자가 있어도 학년으로 파싱하지 않는다")
    void parseCourses_ignoreGradeForCategory() {
        String xmlData = """
                <Dataset id="GRD_COUR001">
                    <Rows>
                        <Row>
                            <Col id="SBJTCD">70007</Col>
                            <Col id="CLSS">01</Col>
                            <Col id="YY">2026</Col>
                            <Col id="SHTM">10</Col>
                            <Col id="TLSNOBJFGNM">전체(학부)</Col>
                            <Col id="SUSTCDNM">공학계열 1 1</Col>
                        </Row>
                    </Rows>
                </Dataset>
                """;

        List<Course> courses = parser.parseCourses(xmlData);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getTargetGrade()).isEqualTo(TargetGrade.ALL);
    }
}
