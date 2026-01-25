package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.CourseAccreditation;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class JbnuCourseParser {

    private static final String DATASET_ID = "GRD_COUR001";

    public List<Course> parseCourses(String xmlData) {
        List<Course> courseList = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", Parser.xmlParser());

        Elements rows = doc.select("Dataset[id=" + DATASET_ID + "] > Rows > Row");

        for (Element row : rows) {
            try {
                processRow(row).ifPresent(courseList::add);
            } catch (Exception e) {
                log.warn("Failed to parse row: {}", e.getMessage());
            }
        }
        return courseList;
    }

    private Optional<Course> processRow(Element row) {
        String sbjtCd = getColValue(row, "SBJTCD");
        String clss = getColValue(row, "CLSS");
        String sbjtNm = getColValue(row, "SBJTNM");
        String profNm = getColValue(row, "RPSTPROFNM");
        String targetGrade = getColValue(row, "TLSNOBJFGNM"); // 수강대상 정보
        String year = getColValue(row, "YY");
        String semester = getColValue(row, "SHTM");
        String classificationStr = getColValue(row, "CPTNFGNM");
        String department = getColValue(row, "SUSTCDNM");
        String gradingMethodStr = getColValue(row, "SCORTRETFGNM");
        String lectureLanguageStr = getColValue(row, "LTLANGFGNM");
        String classTime = getColValue(row, "DAYTMCTNT");
        String credits = getColValue(row, "PNT");

        int lmtrCnt = safeParseInt(getColValue(row, "LMTRCNT"));
        int tlsnrCnt = safeParseInt(getColValue(row, "TLSNRCNT"));

        if (sbjtCd == null || clss == null) {
            return Optional.empty();
        }

        String lectureHoursStr = getColValue(row, "TM");
        int lectureHours = safeParseInt(lectureHoursStr);

        String generalCategory = getColValue(row, "FLDFGNM");
        String generalDetail = getColValue(row, "FLDDETAFGNM");
        String accreditationStr = getColValue(row, "VLDFGNM");
        String statusStr = getColValue(row, "OPENLECTFGNM");
        String classroom = getColValue(row, "VILROOMNOCTNT");
        String hasSyllabusStr = getColValue(row, "SUBPLANYN");
        boolean hasSyllabus = "Y".equalsIgnoreCase(hasSyllabusStr);

        String courseDirection = getColValue(row, "CLSSOPRTDRCT");
        String classDuration = getColValue(row, "LESSTMFGNM");

        String disclosureStr = getColValue(row, "PUBCYN");
        DisclosureStatus disclosure = DisclosureStatus.from(disclosureStr);
        String disclosureReason = getColValue(row, "NOPUBCRESNNM");

        Course course = Course.builder()
                .courseKey(year + "-" + semester + "-" + sbjtCd + "-" + clss)
                .subjectCode(sbjtCd)
                .classNumber(clss)
                .name(sbjtNm)
                .professor(profNm)
                .capacity(lmtrCnt)
                .current(tlsnrCnt)
                .targetGrade(targetGrade)
                .academicYear(year)
                .semester(semester)
                .classification(CourseClassification.from(classificationStr))
                .department(department)
                .gradingMethod(GradingMethod.from(gradingMethodStr))
                .lectureLanguage(LectureLanguage.from(lectureLanguageStr))
                .classTime(classTime)
                .credits(credits)
                .disclosure(disclosure)
                .disclosureReason(disclosureReason)
                .lectureHours(lectureHours)
                .generalCategory(generalCategory)
                .generalDetail(generalDetail)
                .accreditation(CourseAccreditation.from(accreditationStr))
                .status(CourseStatus.from(statusStr))
                .classroom(classroom)
                .hasSyllabus(hasSyllabus)
                .generalCategoryByYear(getColValue(row, "FLDCONVINFO"))
                .courseDirection(courseDirection)
                .classDuration(classDuration)
                .build();

        List<CourseSchedule> schedules = parseSchedules(classTime);
        for (CourseSchedule schedule : schedules) {
            course.addSchedule(schedule);
        }

        return Optional.of(course);
    }

    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        return col != null ? col.text() : null;
    }

    private int safeParseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<CourseSchedule> parseSchedules(String timeString) {
        List<CourseSchedule> schedules = new ArrayList<>();
        if (timeString == null || timeString.isBlank()) {
            return schedules;
        }

        // Example input: "월 6-A,월 6-B,수 6-A,수 6-B"
        String[] tokens = timeString.split(",");
        for (String token : tokens) {
            token = token.trim();
            // Expected format: "월 6-A"
            String[] parts = token.split(" ");
            if (parts.length < 2)
                continue;

            String dayStr = parts[0];
            String periodStr = parts[1];

            CourseDayOfWeek day = CourseDayOfWeek.from(dayStr);
            ClassPeriod period = ClassPeriod.from(periodStr);

            if (day != null && period != null) {
                schedules.add(CourseSchedule.builder()
                        .dayOfWeek(day)
                        .period(period)
                        .build());
            }
        }
        return schedules;
    }
}
