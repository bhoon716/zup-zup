package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import bhoon.sugang_helper.domain.course.entity.Department;
import bhoon.sugang_helper.domain.course.enums.CourseAccreditation;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import bhoon.sugang_helper.domain.course.repository.DepartmentRepository;
import bhoon.sugang_helper.domain.course.util.JbnuDepartmentStandardizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * 전북대학교 오아시스 시스템의 강의 데이터를 파싱하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JbnuCourseParser {

    private final DepartmentRepository departmentRepository;

    private static final Pattern PERIOD_TOKEN_PATTERN = Pattern.compile("^(\\d{1,2})-([ABab])$");
    private static final Pattern GRADE_IN_DEPT_PATTERN = Pattern.compile("\\s(?<grade>[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");

    /**
     * XML 데이터를 파싱하여 강의 엔티티 목록으로 변환
     */
    public List<Course> parseCourses(String xmlData) {
        List<Course> courses = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", org.jsoup.parser.Parser.xmlParser());
        Elements rows = doc.select("Dataset[id=GRD_COUR001] Row");

        for (Element row : rows) {
            courses.add(parseCourseRow(row));
        }

        return courses;
    }

    /**
     * XML의 개별 Row 엘리먼트를 Course 엔티티로 변환
     */
    private Course parseCourseRow(Element row) {
        String academicYear = getColValue(row, "YY");
        String semester = getColValue(row, "SHTM");
        String subjectCode = getColValue(row, "SBJTCD");
        String clss = getColValue(row, "CLSS");
        String rawDepartment = getColValue(row, "SUSTCDNM");
        String tlsnObjFgnm = getColValue(row, "TLSNOBJFGNM");

        TargetGrade targetGrade = parseTargetGrade(tlsnObjFgnm, rawDepartment);
        String subjectName = normalizeSubjectName(getColValue(row, "SBJTNM"), clss);
        String department = normalizeDepartmentName(rawDepartment, targetGrade);

        // 학과 명칭 기반으로 ID 매핑 수행
        DepartmentIds ids = mapDepartmentIds(department, rawDepartment);

        Course course = buildCourseEntity(row, academicYear, semester, subjectCode, clss, targetGrade, subjectName,
                department, ids.collegeId(), ids.departmentId());

        List<CourseSchedule> schedules = parseSchedules(getColValue(row, "DAYTMCTNT"));
        if (schedules != null) {
            schedules.forEach(course::addSchedule);
        }

        return course;
    }

    /**
     * 정규화된 학과명을 기반으로 단과대 및 학과 ID를 매핑
     */
    private DepartmentIds mapDepartmentIds(String department, String rawDepartment) {
        if (department == null) {
            return new DepartmentIds(null, null);
        }

        // 복수 학과인 경우(콤마로 구분됨) 첫 번째 학과를 기준으로 매핑 시도
        String primaryDepartment = department.split(",")[0].trim();
        return departmentRepository.findByName(primaryDepartment)
                .map(dept -> new DepartmentIds(dept.getCollege().getId(), dept.getId()))
                .orElseGet(() -> {
                    log.warn("[Crawler] 미매핑 학과 발견: '{}' (원본: '{}')", primaryDepartment, rawDepartment);
                    return new DepartmentIds(null, null);
                });
    }

    /**
     * 매핑된 ID 정보를 담는 내부 레코드
     */
    private record DepartmentIds(Long collegeId, Long departmentId) {
    }

    /**
     * 파싱된 데이터를 바탕으로 Course 엔티티 빌드
     */
    private Course buildCourseEntity(Element row, String academicYear, String semester, String subjectCode, String clss,
            TargetGrade targetGrade, String subjectName, String department, Long collegeId, Long departmentId) {
        return Course.builder()
                .courseKey(String.format("%s:%s:%s:%s", academicYear, semester, subjectCode, clss))
                .subjectCode(subjectCode)
                .name(subjectName)
                .classNumber(clss)
                .professor(getColValue(row, "RPSTPROFNM"))
                .capacity(getSafeInt(getColValue(row, "LMTRCNT")))
                .current(getSafeInt(getColValue(row, "TLSNRCNT")))
                .targetGrade(targetGrade)
                .academicYear(academicYear)
                .semester(semester)
                .classification(CourseClassification.from(getColValue(row, "CPTNFGNM")))
                .department(department)
                .collegeId(collegeId)
                .departmentId(departmentId)
                .gradingMethod(GradingMethod.from(getColValue(row, "SCORTRETFGNM")))
                .classTime(getColValue(row, "DAYTMCTNT"))
                .credits(getColValue(row, "PNT"))
                .lectureLanguage(LectureLanguage.from(getColValue(row, "LTLANGFGNM")))
                .disclosure(DisclosureStatus.from(getColValue(row, "PUBCYN")))
                .disclosureReason(getColValue(row, "NOPUBCRESNNM"))
                .lectureHours(getSafeInt(getColValue(row, "TM")))
                .generalCategory(getColValue(row, "FLDFGNM"))
                .generalDetail(getColValue(row, "FLDDETAFGNM"))
                .accreditation(CourseAccreditation.from(getColValue(row, "VLDFGNM")))
                .status(CourseStatus.from(getColValue(row, "OPENLECTFGNM")))
                .classroom(getColValue(row, "VILROOMNOCTNT"))
                .hasSyllabus("Y".equalsIgnoreCase(getColValue(row, "SUBPLANYN")))
                .generalCategoryByYear(getColValue(row, "FLDCONVINFO"))
                .courseDirection(getColValue(row, "OPENLECTFGNM"))
                .classDuration(getColValue(row, "LECT_QU_FGNM"))
                .build();
    }

    private TargetGrade parseTargetGrade(String tlsnObjFgnm, String deptNm) {
        TargetGrade fromTlsn = TargetGrade.from(tlsnObjFgnm);
        if (fromTlsn != null) {
            if (deptNm != null && deptNm.contains("계열") && deptNm.contains("1")) {
                return TargetGrade.GRADE_1;
            }
            return fromTlsn;
        }

        if (deptNm == null) {
            return null;
        }

        Matcher matcher = GRADE_IN_DEPT_PATTERN.matcher(deptNm);
        String lastMatchedGrade = null;
        while (matcher.find()) {
            lastMatchedGrade = matcher.group("grade");
        }

        return lastMatchedGrade != null ? TargetGrade.from(lastMatchedGrade) : null;
    }

    private String normalizeSubjectName(String name, String classNumber) {
        if (name == null || classNumber == null) {
            return name;
        }

        String normalizedClassNumber = classNumber.replaceFirst("^0+(?!$)", "");
        Matcher matcher = TRAILING_NUMBER_IN_SUBJECT_PATTERN.matcher(name);

        if (matcher.matches()) {
            String number = matcher.group("number");
            if (number.equals(normalizedClassNumber)) {
                return matcher.group("subjectName").trim();
            }
        }

        return name.trim();
    }

    private String normalizeDepartmentName(String rawDepartment, TargetGrade targetGrade) {
        if (rawDepartment == null || rawDepartment.isBlank()) {
            return null;
        }

        String gradeNumber = extractGradeNumber(targetGrade);
        String[] tokens = rawDepartment.split(",");
        List<String> normalizedTokens = new ArrayList<>();

        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            String normalizedToken = JbnuDepartmentStandardizer.normalize(trimmedToken, gradeNumber);
            if (normalizedToken != null && !normalizedToken.isBlank()) {
                normalizedTokens.add(normalizedToken);
            }
        }

        return normalizedTokens.isEmpty() ? rawDepartment.trim() : String.join(", ", normalizedTokens);
    }

    private String extractGradeNumber(TargetGrade targetGrade) {
        if (targetGrade == null) {
            return null;
        }
        return targetGrade.name().replace("GRADE_", "");
    }

    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        if (col == null) {
            return null;
        }
        String value = col.text().trim();
        return (value.isEmpty() || value.equals(":")) ? null : value;
    }

    private Integer getSafeInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public List<CourseSchedule> parseSchedules(String timeContent) {
        if (timeContent == null || timeContent.isBlank()) {
            return List.of();
        }

        List<CourseSchedule> schedules = new ArrayList<>();
        String[] tokens = timeContent.split(",");

        for (String token : tokens) {
            parseAndAddScheduleTokens(token.trim(), schedules);
        }

        return mergeConsecutiveSchedules(schedules);
    }

    private void parseAndAddScheduleTokens(String token, List<CourseSchedule> schedules) {
        if (token.isEmpty()) {
            return;
        }

        String[] parts = token.split("\\s+");
        if (parts.length < 2) {
            return;
        }

        CourseDayOfWeek dayOfWeek = CourseDayOfWeek.from(parts[0]);
        if (dayOfWeek == null) {
            return;
        }

        for (int i = 1; i < parts.length; i++) {
            Matcher matcher = PERIOD_TOKEN_PATTERN.matcher(parts[i]);
            if (matcher.matches()) {
                addScheduleFromMatcher(matcher, dayOfWeek, schedules);
            }
        }
    }

    private void addScheduleFromMatcher(Matcher matcher, CourseDayOfWeek dayOfWeek, List<CourseSchedule> schedules) {
        int period = Integer.parseInt(matcher.group(1));
        String subPeriod = matcher.group(2).toUpperCase();

        LocalTime startTime = calculateStartTime(period, subPeriod);
        LocalTime endTime = calculateEndTime(period, subPeriod);

        if (startTime != null && endTime != null) {
            schedules.add(new CourseSchedule(dayOfWeek, startTime, endTime));
        }
    }

    private List<CourseSchedule> mergeConsecutiveSchedules(List<CourseSchedule> schedules) {
        if (schedules.size() <= 1) {
            return schedules;
        }

        schedules.sort((a, b) -> {
            if (a.getDayOfWeek() != b.getDayOfWeek()) {
                return a.getDayOfWeek().compareTo(b.getDayOfWeek());
            }
            return a.getStartTime().compareTo(b.getStartTime());
        });

        List<CourseSchedule> merged = new ArrayList<>();
        CourseSchedule current = schedules.get(0);

        for (int i = 1; i < schedules.size(); i++) {
            CourseSchedule next = schedules.get(i);
            if (current.getDayOfWeek() == next.getDayOfWeek() && current.getEndTime().equals(next.getStartTime())) {
                current = new CourseSchedule(current.getDayOfWeek(), current.getStartTime(), next.getEndTime());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private LocalTime calculateStartTime(int period, String subPeriod) {
        int hour = 8 + period;
        int minute = subPeriod.equals("A") ? 0 : 30;
        return (hour >= 24) ? LocalTime.MAX : LocalTime.of(hour, minute);
    }

    private LocalTime calculateEndTime(int period, String subPeriod) {
        int hour = 8 + period;
        int minute = subPeriod.equals("A") ? 30 : 0;
        if (subPeriod.equals("B")) {
            hour++;
        }

        return (hour >= 24) ? LocalTime.of(23, 59, 59) : LocalTime.of(hour, minute);
    }
}
