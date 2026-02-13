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
        String year = getColValue(row, "YY");
        String semester = getColValue(row, "SHTM");

        if (sbjtCd == null || clss == null || year == null || semester == null) {
            return Optional.empty();
        }

        LiberalArtsInfo liberalArts = parseLiberalArtsInfo(row);
        StatusInfo statusInfo = parseStatusAndDisclosureInfo(row);

        Course course = Course.builder()
                .courseKey(generateCourseKey(year, semester, sbjtCd, clss))
                .subjectCode(sbjtCd)
                .classNumber(clss)
                .name(getColValue(row, "SBJTNM"))
                .professor(getColValue(row, "RPSTPROFNM"))
                .capacity(safeParseInt(getColValue(row, "LMTRCNT")))
                .current(safeParseInt(getColValue(row, "TLSNRCNT")))
                .targetGrade(getColValue(row, "TLSNOBJFGNM"))
                .academicYear(year)
                .semester(semester)
                .classification(CourseClassification.from(getColValue(row, "CPTNFGNM")))
                .department(getColValue(row, "SUSTCDNM"))
                .gradingMethod(GradingMethod.from(getColValue(row, "SCORTRETFGNM")))
                .lectureLanguage(LectureLanguage.from(getColValue(row, "LTLANGFGNM")))
                .classTime(getColValue(row, "DAYTMCTNT"))
                .credits(getColValue(row, "PNT"))
                .disclosure(statusInfo.disclosure)
                .disclosureReason(statusInfo.disclosureReason)
                .lectureHours(safeParseInt(getColValue(row, "TM")))
                .generalCategory(liberalArts.category)
                .generalDetail(liberalArts.detail)
                .accreditation(CourseAccreditation.from(getColValue(row, "VLDFGNM")))
                .status(CourseStatus.from(getColValue(row, "OPENLECTFGNM")))
                .classroom(getColValue(row, "VILROOMNOCTNT"))
                .hasSyllabus("Y".equalsIgnoreCase(getColValue(row, "SUBPLANYN")))
                .generalCategoryByYear(getColValue(row, "FLDCONVINFO"))
                .courseDirection(getColValue(row, "CLSSOPRTDRCT"))
                .classDuration(getColValue(row, "LESSTMFGNM"))
                .build();

        parseSchedules(getColValue(row, "DAYTMCTNT")).forEach(course::addSchedule);

        return Optional.of(course);
    }

    private String generateCourseKey(String year, String semester, String sbjtCd, String clss) {
        return String.format("%s:%s:%s:%s", year, semester, sbjtCd, clss);
    }

    private LiberalArtsInfo parseLiberalArtsInfo(Element row) {
        String category = getColValue(row, "FLDFGNM");
        String detail = getColValue(row, "FLDDETAFGNM");
        String convInfo = getColValue(row, "FLDCONVINFO");

        if (convInfo != null && convInfo.contains(",")) {
            String[] parts = convInfo.split(",");
            if (parts.length >= 2) {
                category = parts[0].trim();
                detail = parts[1].trim();
            }
        }
        return new LiberalArtsInfo(category, detail);
    }

    private StatusInfo parseStatusAndDisclosureInfo(Element row) {
        DisclosureStatus disclosure = DisclosureStatus.from(getColValue(row, "PUBCYN"));
        String disclosureReason = getColValue(row, "NOPUBCRESNNM");
        return new StatusInfo(disclosure, disclosureReason);
    }

    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        return col != null ? col.text().trim() : null;
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

        String[] tokens = timeString.split(",");
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty())
                continue;

            CourseSchedule schedule = parseCourseSchedule(trimmedToken.split("\\s+"));
            if (schedule == null)
                continue;
            schedules.add(schedule);
        }
        return schedules;
    }

    private CourseSchedule parseCourseSchedule(String[] parts) {
        if (parts.length < 2) {
            return null;
        }

        CourseDayOfWeek day = CourseDayOfWeek.from(parts[0]);
        ClassPeriod period = ClassPeriod.from(parts[1]);

        if (day == null || period == null) {
            return null;
        }

        return CourseSchedule.builder()
                .dayOfWeek(day)
                .period(period)
                .build();
    }

    private record LiberalArtsInfo(String category, String detail) {
    }

    private record StatusInfo(DisclosureStatus disclosure, String disclosureReason) {
    }
}
