package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JbnuCourseParserTest {

    private final JbnuCourseParser parser = new JbnuCourseParser();

    @Test
    @DisplayName("JBNU 수강신청 XML 데이터 파싱 성공")
    void parseCourses_success() {
        // given
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Root>\n" +
                "  <Dataset id=\"GRD_COUR001\">\n" +
                "    <Rows>\n" +
                "      <Row>\n" +
                "        <Col id=\"SBJTCD\">12345</Col>\n" +
                "        <Col id=\"CLSS\">01</Col>\n" +
                "        <Col id=\"SBJTNM\">테스트과목1</Col>\n" +
                "        <Col id=\"RPSTPROFNM\">홍길동</Col>\n" +
                "        <Col id=\"LMTRCNT\">40</Col>\n" +
                "        <Col id=\"TLSNRCNT\">35</Col>\n" +
                "      </Row>\n" +
                "      <Row>\n" +
                "        <Col id=\"SBJTCD\">67890</Col>\n" +
                "        <Col id=\"CLSS\">02</Col>\n" +
                "        <Col id=\"SBJTNM\">테스트과목2</Col>\n" +
                "        <Col id=\"RPSTPROFNM\">김철수</Col>\n" +
                "        <Col id=\"LMTRCNT\">30</Col>\n" +
                "        <Col id=\"TLSNRCNT\">30</Col>\n" +
                "      </Row>\n" +
                "    </Rows>\n" +
                "  </Dataset>\n" +
                "</Root>";

        // when
        List<Course> courses = parser.parseCourses(xmlData);

        // then
        assertThat(courses).hasSize(2);

        Course course1 = courses.stream()
                .filter(c -> c.getCourseKey().equals("12345-01"))
                .findFirst()
                .orElseThrow();
        assertThat(course1.getName()).isEqualTo("테스트과목1");
        assertThat(course1.getProfessor()).isEqualTo("홍길동");
        assertThat(course1.getCapacity()).isEqualTo(40);
        assertThat(course1.getCurrent()).isEqualTo(35);
        assertThat(course1.getAvailable()).isEqualTo(5);

        Course course2 = courses.stream()
                .filter(c -> c.getCourseKey().equals("67890-02"))
                .findFirst()
                .orElseThrow();
        assertThat(course2.getAvailable()).isZero();
    }

    @Test
    @DisplayName("필수 데이터(과목코드, 분반) 누락 시 해당 과목 제외")
    void parseCourses_skip_invalid_row() {
        // given
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Root>\n" +
                "  <Dataset id=\"GRD_COUR001\">\n" +
                "    <Rows>\n" +
                "      <Row>\n" +
                "        <Col id=\"SBJTCD\">12345</Col>\n" +
                "        <Col id=\"SBJTNM\">분반누락과목</Col>\n" +
                "      </Row>\n" +
                "    </Rows>\n" +
                "  </Dataset>\n" +
                "</Root>";

        // when
        List<Course> courses = parser.parseCourses(xmlData);

        // then
        assertThat(courses).isEmpty();
    }

    @Test
    @DisplayName("숫자 데이터 파싱 실패 시 0으로 처리")
    void parseCourses_invalid_number_format() {
        // given
        String xmlData = "<Root><Dataset id=\"GRD_COUR001\"><Rows><Row>" +
                "<Col id=\"SBJTCD\">12345</Col>" +
                "<Col id=\"CLSS\">01</Col>" +
                "<Col id=\"LMTRCNT\">INVALID</Col>" +
                "</Row></Rows></Dataset></Root>";

        // when
        List<Course> courses = parser.parseCourses(xmlData);

        // then
        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getCapacity()).isZero();
    }
}
