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
import bhoon.sugang_helper.domain.course.repository.DepartmentRepository;
import bhoon.sugang_helper.domain.course.util.JbnuDepartmentStandardizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private static final Pattern GRADE_IN_DEPT_PATTERN = Pattern.compile("(?<=[\\s,])(?<grade>[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");

    /** XML 데이터를 파싱하여 강의 엔티티 목록으로 변환 */
    public List<Course> parseCourses(String xmlData) {
        List<Course> courses = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", org.jsoup.parser.Parser.xmlParser());
        Elements rows = doc.select("Dataset[id=GRD_COUR001] Row");

        for (Element row : rows) {
            courses.add(parseCourseRow(row));
        }

        return courses;
    }

    /** XML의 개별 Row를 Course 엔티티로 변환 */
    private Course parseCourseRow(Element row) {
        String academicYear = getColValue(row, "YY");
        String semester = getColValue(row, "SHTM");
        String subjectCode = getColValue(row, "SBJTCD");
        String clss = getColValue(row, "CLSS");

        String rawDepartment = getColValue(row, "SUSTCDNM");
        String tlsnObjFgnm = getColValue(row, "TLSNOBJFGNM");
        
        TargetGrade targetGrade = parseTargetGrade(tlsnObjFgnm, rawDepartment);
        String subjectName = normalizeSubjectName(getColValue(row, "SBJTNM"), clss);
        String department = normalizeDepartmentName(rawDepartment);

        DepartmentIds ids = mapDepartmentIds(department, rawDepartment);

        Course course = buildCourseEntity(row, academicYear, semester, subjectCode, clss, targetGrade, subjectName,
                department, ids);

        addSchedulesToCourse(row, course);

        return course;
    }

    /** 표준화된 학과명을 기반으로 단과대 및 학과 ID 매핑 */
    private DepartmentIds mapDepartmentIds(String department, String rawDepartment) {
        if (department == null || department.isBlank()) {
            return new DepartmentIds(null, null);
        }

        String[] tokens = department.split(",");
        for (String token : tokens) {
            String target = token.trim();
            if (target.isEmpty()) {
                continue;
            }

            Optional<DepartmentIds> result = tryMap(target);
            if (result.isPresent()) {
                return result.get();
            }

            // 괄호 제거 후 재시도
            String stripped = JbnuDepartmentStandardizer.PARENTHESES_PATTERN.matcher(target).replaceAll("").trim();
            if (!stripped.equals(target)) {
                result = tryMap(stripped);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        }

        log.warn("[Crawler] Unmapped department found: '{}' (Raw: '{}')", department, rawDepartment);
        return new DepartmentIds(null, null);
    }

    /** 부서 명칭으로 DB에서 ID 조회 */
    private Optional<DepartmentIds> tryMap(String name) {
        return departmentRepository.findByName(name)
                .map(dept -> new DepartmentIds(dept.getCollege().getId(), dept.getId()));
    }

    /** 학과 및 단과대 ID 보관용 레코드 */
    private record DepartmentIds(Long collegeId, Long departmentId) {
    }

    /** Course 엔티티 빌더 로직 수행 */
    private Course buildCourseEntity(Element row, String academicYear, String semester, String subjectCode, String clss,
            TargetGrade targetGrade, String subjectName, String department, DepartmentIds ids) {
        return Course.builder()
                .courseKey(generateCourseKey(academicYear, semester, subjectCode, clss))
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
                .collegeId(ids.collegeId())
                .departmentId(ids.departmentId())
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

    /** 강좌 식별용 고유 키 생성 */
    private String generateCourseKey(String year, String semester, String code, String clss) {
        return String.format("%s:%s:%s:%s", year, semester, code, clss);
    }

    /** 엔티티에 파싱된 스케줄 정보 추가 */
    private void addSchedulesToCourse(Element row, Course course) {
        List<CourseSchedule> schedules = parseSchedules(getColValue(row, "DAYTMCTNT"));
        if (schedules != null) {
            schedules.forEach(course::addSchedule);
        }
    }

    /** 대상 학년 정보 파싱 */
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

        Matcher matcher = GRADE_IN_DEPT_PATTERN.matcher(" " + deptNm + " ");
        String lastMatchedGrade = null;
        while (matcher.find()) {
            lastMatchedGrade = matcher.group("grade");
        }

        return lastMatchedGrade != null ? TargetGrade.from(lastMatchedGrade) : null;
    }

    /** 과목명 정규화 (불필요한 분반 번호 제거) */
    private String normalizeSubjectName(String name, String classNumber) {
        if (name == null || classNumber == null) {
            return name;
        }

        String normalizedClassNumber = classNumber.replaceFirst("^0+(?!$)", "");
        Matcher matcher = TRAILING_NUMBER_IN_SUBJECT_PATTERN.matcher(name);

        if (matcher.matches()) {
            String number = matcher.group("number").replaceFirst("^0+(?!$)", "");
            if (number.equals(normalizedClassNumber)) {
                return matcher.group("subjectName").trim();
            }
        }

        return name.trim();
    }

    /** 학과 명칭 표준화 유틸 호출 */
    private String normalizeDepartmentName(String rawDepartment) {
        if (rawDepartment == null || rawDepartment.isBlank()) {
            return null;
        }

        String[] tokens = rawDepartment.split(",");
        List<String> normalizedTokens = new ArrayList<>();

        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) continue;

            String normalizedToken = JbnuDepartmentStandardizer.standardize(trimmedToken);
            if (normalizedToken != null && !normalizedToken.isBlank()) {
                normalizedTokens.add(normalizedToken);
            }
        }

        return normalizedTokens.isEmpty() ? "" : String.join(", ", normalizedTokens);
    }

    /** XML Row에서 특정 컬럼의 텍스트 추출 */
    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        if (col == null) return null;
        String value = col.text().trim();
        return (value.isEmpty() || value.equals(":")) ? null : value;
    }

    /** 문자열을 정수로 안전하게 변환 */
    private Integer getSafeInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 강의 시간 문자열을 스케줄 목록으로 파싱 */
    public List<CourseSchedule> parseSchedules(String timeContent) {
        if (timeContent == null || timeContent.isBlank()) {
            return List.of();
        }

        List<CourseSchedule> schedules = new ArrayList<>();
        String[] tokens = timeContent.split(",");
        CourseDayOfWeek lastDay = null;

        for (String token : tokens) {
            lastDay = parseTokenAndAddSchedules(token.trim(), schedules, lastDay);
        }

        return mergeConsecutiveSchedules(schedules);
    }

    /** 시간 토큰 분석 및 스케줄 추가 */
    private CourseDayOfWeek parseTokenAndAddSchedules(String token, List<CourseSchedule> schedules, CourseDayOfWeek lastDay) {
        if (token.isEmpty()) {
            return lastDay;
        }

        String[] parts = token.split("\\s+");
        CourseDayOfWeek currentDay = CourseDayOfWeek.from(parts[0]);
        int periodStartIndex = (currentDay != null) ? 1 : 0;

        if (currentDay == null) {
            currentDay = lastDay;
        }

        if (currentDay == null) {
            return null;
        }

        for (int i = periodStartIndex; i < parts.length; i++) {
            parsePeriodAndAddSchedule(parts[i], currentDay, schedules);
        }
        
        return currentDay;
    }

    /** 교시 정보를 파싱하여 스케줄 객체 생성 */
    private void parsePeriodAndAddSchedule(String periodToken, CourseDayOfWeek dayOfWeek, List<CourseSchedule> schedules) {
        Matcher matcher = PERIOD_TOKEN_PATTERN.matcher(periodToken);
        if (!matcher.matches()) {
            return;
        }

        int period = Integer.parseInt(matcher.group(1));
        String subPeriod = matcher.group(2).toUpperCase();

        LocalTime startTime = calculateStartTime(period, subPeriod);
        LocalTime endTime = calculateEndTime(period, subPeriod);

        if (startTime != null && endTime != null) {
            schedules.add(new CourseSchedule(dayOfWeek, startTime, endTime));
        }
    }

    /** 연속된 교시 병합 처리 */
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

    /** 교시별 시작 시간 계산 */
    private LocalTime calculateStartTime(int period, String subPeriod) {
        int hour = 8 + period;
        int minute = "A".equals(subPeriod) ? 0 : 30;
        return (hour >= 24) ? LocalTime.MAX : LocalTime.of(hour, minute);
    }

    /** 교시별 종료 시간 계산 */
    private LocalTime calculateEndTime(int period, String subPeriod) {
        int hour = 8 + period;
        int minute = "A".equals(subPeriod) ? 30 : 0;
        if ("B".equals(subPeriod)) {
            hour++;
        }
        return (hour >= 24) ? LocalTime.of(23, 59, 59) : LocalTime.of(hour, minute);
    }
}
