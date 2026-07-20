package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.TargetGrade;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JbnuCourseParserTest {

    private final JbnuCourseParser parser = new JbnuCourseParser();

    @Test
    @DisplayName("JUMP JSON에서 내부 코드와 학수번호를 분리해 파싱합니다")
    void parseCourses_jumpJson() {
        List<ParsedCourseDto> courses = parser.parseCourses(jsonResponse("""
                {
                  "SBJCT_CD": "C101",
                  "SBJCT_NM": "자료구조 1",
                  "DVCLS_NO": "01",
                  "STDTR_NO": "  geco178 ",
                  "STAFFNM": "김교수",
                  "PRM_NMPR_CNT": 40,
                  "ATNLC_PSCP_CNT": 40,
                  "TLSN_RCNT": 12,
                  "ATNLC_TRGT_DIVCDNM": "3학년",
                  "YRSA": "2026",
                  "SEMSTR_CD": "SUSR016.010",
                  "CMCRS_DIVCDNM": "전공",
                  "RLT_ABSLT_EVL_DIVCDNM": "절대평가",
                  "DOW_HR_CN": "월 1-A, 월 1-B",
                  "ESTBL_PNT": 3,
                  "HR_CNT": 3,
                  "LCTR_LANG_DIVCDNM": "한국어",
                  "RLS_YN": "Y",
                  "DONG_RMNM_CN": "공대 301",
                  "RLM_DIVCDNM": "전공",
                  "CULT_RLM_DIVCDNM": "전공필수",
                  "LESSN_OPER_DRC_CN": "일반",
                  "OPNLT_DIVCDNM": "일반",
                  "LESSN_HR_DIVCDNM": "주 3시간",
                  "MNG_SCSBJT_CDNM": "컴퓨터공학부"
                }
                """));

        assertThat(courses).hasSize(1);
        ParsedCourseDto course = courses.get(0);
        assertThat(course.subjectCode()).isEqualTo("C101");
        assertThat(course.stdtrNo()).isEqualTo("GECO178");
        assertThat(course.courseKey()).isEqualTo("2026:SUSR016.010:C101:01");
        assertThat(course.name()).isEqualTo("자료구조");
        assertThat(course.capacity()).isEqualTo(40);
        assertThat(course.current()).isEqualTo(12);
        assertThat(course.targetGrade()).isEqualTo(TargetGrade.GRADE_3);
        assertThat(course.schedules()).hasSize(1);
        assertThat(course.schedules().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(course.schedules().get(0).endTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void streamCoursesReadsJsonRows() {
        Iterator<ParsedCourseDto> iterator = parser.streamCourses(
                new ByteArrayInputStream(jsonResponse("""
                        {
                          "SBJCT_CD": "A001",
                          "DVCLS_NO": 1,
                          "YRSA": "2026",
                          "SEMSTR_CD": "SUSR016.010",
                          "STDTR_NO": "GECO001"
                        }
                        """).getBytes(StandardCharsets.UTF_8)), "2026", "SUSR016.010");

        assertThat(iterator).toIterable().extracting(ParsedCourseDto::stdtrNo).containsExactly("GECO001");
    }

    @Test
    void parseCourses_allowsMissingStdtrNo() {
        ParsedCourseDto course = parser.parseCourses(jsonResponse("""
                {
                  "SBJCT_CD": "A001",
                  "DVCLS_NO": 1,
                  "YRSA": "2026",
                  "SEMSTR_CD": "SUSR016.010"
                }
                """)).get(0);

        assertThat(course.stdtrNo()).isNull();
    }

    @Test
    void parseCourses_rejectsJumpErrorEnvelope() {
        assertThatThrownBy(() -> parser.parseCourses("{\"ERRMSGINFO\":{\"ERRCODE\":\"CCMN002.CCMN@NONE\"}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JUMP returned an error envelope");
    }

    @Test
    void parseCourses_rejectsMissingEnvelope() {
        assertThatThrownBy(() -> parser.parseCourses("{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing dsEstSbjList");
    }

    @Test
    void parseCourses_rejectsEmptyCourseList() {
        assertThatThrownBy(() -> parser.parseCourses("{\"dsEstSbjList\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("course list is empty");
    }

    @Test
    void parseCourses_rejectsMissingRequiredIdentity() {
        assertThatThrownBy(() -> parser.parseCourses(jsonResponse("""
                {
                  "SBJCT_CD": "A001",
                  "DVCLS_NO": 1,
                  "YRSA": "2026"
                }
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required identity fields");
    }

    @Test
    void parseCourses_rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parseCourses("{\"dsEstSbjList\":["))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed crawler JSON");
    }

    @Test
    @DisplayName("대상 학년과 학과의 학년 표기를 분리해 파싱합니다")
    void parseCourses_extractsGradeAndDepartment() {
        ParsedCourseDto course = parser.parseCourses(jsonResponse("""
                {
                  "SBJCT_CD": "A001",
                  "DVCLS_NO": 1,
                  "YRSA": "2026",
                  "SEMSTR_CD": "SUSR016.010",
                  "ATNLC_TRGT_DIVCDNM": "전체(학부)",
                  "SCSBJT_SCYR_NTC_CN": "컴퓨터공학부 3",
                  "MNG_SCSBJT_CDNM": "컴퓨터공학부 3"
                }
                """)).get(0);

        assertThat(course.targetGrade()).isEqualTo(TargetGrade.GRADE_3);
        assertThat(course.department()).isEqualTo("컴퓨터공학부");
    }

    @Test
    void parseCourses_preservesSingleHalfSlot() {
        ParsedCourseDto course = parser.parseCourses(jsonResponse("""
                {
                  "SBJCT_CD": "A001",
                  "DVCLS_NO": 1,
                  "YRSA": "2026",
                  "SEMSTR_CD": "SUSR016.010",
                  "DOW_HR_CN": "화 3-B"
                }
                """)).get(0);

        assertThat(course.schedules()).extracting(ParsedCourseDto.ScheduleDto::dayOfWeek)
                .containsExactly(CourseDayOfWeek.TUESDAY);
        assertThat(course.schedules().get(0).startTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(course.schedules().get(0).endTime()).isEqualTo(LocalTime.of(12, 0));
    }

    private String jsonResponse(String row) {
        return "{\"dsEstSbjList\":[" + row + "]}";
    }
}
