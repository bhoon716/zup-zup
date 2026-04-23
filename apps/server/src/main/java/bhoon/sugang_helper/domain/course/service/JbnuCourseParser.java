package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import bhoon.sugang_helper.domain.course.enums.CourseAccreditation;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import bhoon.sugang_helper.domain.course.util.JbnuDepartmentStandardizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class JbnuCourseParser {

    private static final Pattern PERIOD_TOKEN_PATTERN = Pattern.compile("^(\\d{1,2})-([ABab])$");
    private static final Pattern GRADE_IN_DEPT_PATTERN = Pattern.compile("\\s(?<grade>[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");

    /**
     * XML 데이터를 파싱하여 강의 엔티티 목록으로 변환
     *
     * @param xmlData 오아시스에서 받아온 원본 XML 데이터
     * @return 파싱된 강의 엔티티 리스트
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

        Course course = buildCourseEntity(row, academicYear, semester, subjectCode, clss, targetGrade, subjectName,
                department);

        List<CourseSchedule> schedules = parseSchedules(getColValue(row, "DAYTMCTNT"));
        if (schedules != null) {
            schedules.forEach(course::addSchedule);
        }

        return course;
    }

    /**
     * 파싱된 데이터를 바탕으로 Course 엔티티 빌드
     */
    private Course buildCourseEntity(Element row, String academicYear, String semester, String subjectCode, String clss,
            TargetGrade targetGrade, String subjectName, String department) {
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
                .build();
    }

    /**
     * 대상 학년 정보를 추출 (수강대상 컬럼과 학과명에 포함된 숫자를 조합)
     */
    private TargetGrade parseTargetGrade(String tlsnObjFgnm, String deptNm) {
        TargetGrade fromTlsn = TargetGrade.from(tlsnObjFgnm);
        if (fromTlsn != null) {
            // '환경생명자원계열 1'과 같은 특정 케이스 보정
            if (deptNm != null && deptNm.contains("계열") && deptNm.contains("1")) {
                return TargetGrade.GRADE_1;
            }
            return fromTlsn;
        }

        if (deptNm == null) {
            return null;
        }

        // 학과명 내의 마지막 숫자 추출 (예: '경영 4' -> 4학년)
        Matcher matcher = GRADE_IN_DEPT_PATTERN.matcher(deptNm);
        String lastMatchedGrade = null;
        while (matcher.find()) {
            lastMatchedGrade = matcher.group("grade");
        }

        return lastMatchedGrade != null ? TargetGrade.from(lastMatchedGrade) : null;
    }

    /**
     * 과목명 끝에 붙은 불필요한 분반 숫자 제거 (예: '수학 001' -> '수학')
     */
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

    /**
     * 학과명을 쉼표 단위로 분리하여 각각 표준화 후 재결합
     */
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

    /**
     * XML의 특정 Column 값을 가져오며 강의실 위치 등 특수 문자 정규화
     */
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

    /**
     * 강의 시간표 문자열을 파싱하여 스케줄 리스트로 변환
     */
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

    /**
     * 요일별 시간 토큰 파싱 (예: '월 1-A, 1-B')
     */
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

    /**
     * 연속된 강의 시간을 하나의 스케줄로 병합
     */
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
