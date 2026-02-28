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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JbnuCourseParser {

    private static final String DATASET_ID = "GRD_COUR001";
    private static final Pattern PERIOD_TOKEN_PATTERN = Pattern.compile("^(\\d{1,2})-([ABab])$");
    private static final Pattern GRADE_IN_DEPT_PATTERN = Pattern.compile("\\s(?<grade>[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");
    private static final Pattern TRAILING_GRADE_PATTERN_TEMPLATE = Pattern
            .compile("^(?<before>.*?)(?:\\s+)(?<grade>[1-6])(?:학년)?(?:\\s*등)?$");

    /**
     * XML 데이터를 파싱하여 강의 리스트로 변환
     */
    public List<Course> parseCourses(String xmlData) {
        List<Course> courseList = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", Parser.xmlParser());

        Elements rows = doc.select("Dataset[id=" + DATASET_ID + "] > Rows > Row");

        for (Element row : rows) {
            try {
                processRow(row).ifPresent(courseList::add);
            } catch (Exception e) {
                log.warn("강의 행 파싱에 실패했습니다: {}", e.getMessage());
            }
        }
        return courseList;
    }

    /**
     * 단일 행(Row) 데이터를 파싱하여 강의 객체 생성
     */
    private Optional<Course> processRow(Element row) {
        String sbjtCd = getColValue(row, "SBJTCD");
        String clss = getColValue(row, "CLSS");
        String year = getColValue(row, "YY");
        String semester = getColValue(row, "SHTM");
        String rawDepartment = getColValue(row, "SUSTCDNM");
        String gradeNm = getColValue(row, "TLSNOBJFGNM");

        if (sbjtCd == null || clss == null || year == null || semester == null) {
            return Optional.empty();
        }

        LiberalArtsInfo liberalArts = parseLiberalArtsInfo(row);
        StatusInfo statusInfo = parseStatusAndDisclosureInfo(row);
        TargetGrade targetGrade = parseTargetGrade(gradeNm, rawDepartment);

        String subjectName = normalizeSubjectName(getColValue(row, "SBJTNM"), clss);
        String department = normalizeDepartmentName(rawDepartment, targetGrade);

        Course course = Course.builder()
                .courseKey(generateCourseKey(year, semester, sbjtCd, clss))
                .subjectCode(sbjtCd)
                .classNumber(clss)
                .name(subjectName)
                .professor(getColValue(row, "RPSTPROFNM"))
                .capacity(safeParseInt(getColValue(row, "LMTRCNT")))
                .current(safeParseInt(getColValue(row, "TLSNRCNT")))
                .targetGrade(targetGrade)
                .academicYear(year)
                .semester(semester)
                .classification(CourseClassification.from(getColValue(row, "CPTNFGNM")))
                .department(department)
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

    /**
     * 과목명 끝 숫자가 분반(CLSS)과 동일하면 과목명에서 제거한다.
     */
    private String normalizeSubjectName(String rawSubjectName, String classNumber) {
        if (rawSubjectName == null || rawSubjectName.isBlank()) {
            return rawSubjectName;
        }

        if (classNumber == null || classNumber.isBlank()) {
            return rawSubjectName;
        }

        Matcher matcher = TRAILING_NUMBER_IN_SUBJECT_PATTERN.matcher(rawSubjectName);
        if (!matcher.matches()) {
            return rawSubjectName;
        }

        String suffixNumber = matcher.group("number");
        String normalizedClassNumber = classNumber.replaceFirst("^0+(?!$)", "");
        if (!suffixNumber.equals(normalizedClassNumber)) {
            return rawSubjectName;
        }

        return matcher.group("subjectName").trim();
    }

    /**
     * 학과명 끝의 학년 표기(예: "영어영문 3")를 제거하여 학과명만 보존한다.
     */
    private String normalizeDepartmentName(String rawDepartment, TargetGrade targetGrade) {
        if (rawDepartment == null || rawDepartment.isBlank()) {
            return rawDepartment;
        }

        String gradeNumber = extractGradeNumber(targetGrade);
        if (gradeNumber == null) {
            return rawDepartment.trim();
        }

        List<String> normalizedDepartments = new ArrayList<>();
        for (String token : rawDepartment.split(",")) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            String normalizedToken = removeTrailingGradeToken(trimmedToken, gradeNumber);
            if (!normalizedToken.isBlank()) {
                normalizedDepartments.add(normalizedToken);
            }
        }

        if (normalizedDepartments.isEmpty()) {
            return rawDepartment.trim();
        }

        return String.join(", ", normalizedDepartments);
    }

    private String removeTrailingGradeToken(String departmentToken, String gradeNumber) {
        String current = departmentToken;
        while (true) {
            Matcher matcher = TRAILING_GRADE_PATTERN_TEMPLATE.matcher(current);
            if (!matcher.matches()) {
                return current.trim();
            }

            String suffixGrade = matcher.group("grade");
            if (!gradeNumber.equals(suffixGrade)) {
                return current.trim();
            }

            String before = matcher.group("before");
            if (before == null || before.isBlank()) {
                return current.trim();
            }
            current = before.trim();
        }
    }

    private String extractGradeNumber(TargetGrade targetGrade) {
        if (targetGrade == null || targetGrade == TargetGrade.GRADUATE) {
            return null;
        }

        String name = targetGrade.name();
        if (name.startsWith("GRADE_") && name.length() == "GRADE_".length() + 1) {
            return name.substring("GRADE_".length());
        }
        return null;
    }

    /**
     * 강의 고유 키 생성 (연도:학기:과목코드:분반)
     */
    private String generateCourseKey(String year, String semester, String sbjtCd, String clss) {
        return String.format("%s:%s:%s:%s", year, semester, sbjtCd, clss);
    }

    /**
     * 대상 학년 정보 추출 (기본 컬럼 외에 학과 정보의 학년 필드도 분석)
     */
    private TargetGrade parseTargetGrade(String gradeNm, String deptNm) {
        TargetGrade grade = TargetGrade.from(gradeNm);

        // 구체적인 학년이나 대학원이 파싱되면 즉시 반환
        if (grade != null) {
            return grade;
        }

        if (deptNm == null || deptNm.isBlank()) {
            return null;
        }

        // "계열"이 포함된 경우 1학년으로 간주하여 얼리 리턴
        if (deptNm.contains("계열")) {
            return TargetGrade.GRADE_1;
        }

        // "학과명 3", "학과명 3,학과명 3" 형식에서 마지막 숫자를 학년으로 간주
        Matcher matcher = GRADE_IN_DEPT_PATTERN.matcher(deptNm);
        String lastMatchedGrade = null;
        while (matcher.find()) {
            lastMatchedGrade = matcher.group("grade");
        }

        if (lastMatchedGrade != null) {
            return TargetGrade.from(lastMatchedGrade);
        }

        return null;
    }

    /**
     * 교양 과목 정보(영역, 상세영역) 추출
     */
    private LiberalArtsInfo parseLiberalArtsInfo(Element row) {
        String category = getColValue(row, "FLDFGNM");
        String detail = getColValue(row, "FLDDETAFGNM");
        return new LiberalArtsInfo(category, detail);
    }

    /**
     * 강의 상태 및 공개 여부 정보 추출
     */
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

    /**
     * 시간 정보 문자열을 파싱하여 시간표 리스트 생성
     */
    private List<CourseSchedule> parseSchedules(String timeString) {
        List<CourseSchedule> schedules = new ArrayList<>();
        if (timeString == null || timeString.isBlank()) {
            return schedules;
        }

        String[] tokens = timeString.split(",");
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            CourseSchedule schedule = parseCourseSchedule(trimmedToken.split("\\s+"));
            if (schedule == null) {
                continue;
            }
            schedules.add(schedule);
        }
        return mergeConsecutiveSchedules(schedules);
    }

    private CourseSchedule parseCourseSchedule(String[] parts) {
        if (parts.length < 2) {
            return null;
        }

        CourseDayOfWeek day = CourseDayOfWeek.from(parts[0]);
        TimeRange timeRange = parseTimeRange(parts[1]);

        if (day == null || timeRange == null) {
            return null;
        }

        return CourseSchedule.builder()
                .dayOfWeek(day)
                .startTime(timeRange.start())
                .endTime(timeRange.end())
                .build();
    }

    /**
     * 연속된 시간대 정보를 하나의 시간대로 병합 (예: 1A, 1B -> 1시간)
     */
    private List<CourseSchedule> mergeConsecutiveSchedules(List<CourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return schedules;
        }

        List<CourseSchedule> sorted = schedules.stream()
                .sorted(Comparator
                        .comparing((CourseSchedule schedule) -> schedule.getDayOfWeek().ordinal())
                        .thenComparing(CourseSchedule::getStartTime))
                .toList();

        List<CourseSchedule> merged = new ArrayList<>();
        CourseSchedule current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            CourseSchedule next = sorted.get(i);
            if (current.getDayOfWeek() == next.getDayOfWeek()
                    && current.getEndTime().equals(next.getStartTime())) {
                current = CourseSchedule.builder()
                        .dayOfWeek(current.getDayOfWeek())
                        .startTime(current.getStartTime())
                        .endTime(next.getEndTime())
                        .build();
                continue;
            }

            merged.add(current);
            current = next;
        }

        merged.add(current);
        return merged;
    }

    /**
     * 교시 토큰(예: 1-A)을 파싱하여 시간 범위로 변환
     */
    private TimeRange parseTimeRange(String periodToken) {
        if (periodToken == null) {
            return null;
        }

        Matcher matcher = PERIOD_TOKEN_PATTERN.matcher(periodToken.trim());
        if (!matcher.matches()) {
            return null;
        }

        int slot = Integer.parseInt(matcher.group(1));
        if (slot < 0 || slot > 15) {
            return null;
        }

        String half = matcher.group(2).toUpperCase();
        int hour = 8 + slot;

        // A 교시는 정각부터 30분간
        if ("A".equals(half)) {
            return new TimeRange(LocalTime.of(hour, 0), LocalTime.of(hour, 30));
        }

        // B 교시는 30분부터 다음 정각까지 (15교시는 예외 처리)
        if (slot == 15) {
            return new TimeRange(LocalTime.of(23, 30), LocalTime.of(23, 59));
        }
        return new TimeRange(LocalTime.of(hour, 30), LocalTime.of(hour + 1, 0));
    }

    /**
     * 교양 과목 정보를 담는 내부 레코드
     */
    private record LiberalArtsInfo(String category, String detail) {
    }

    /**
     * 강의 상태 정보를 담는 내부 레코드
     */
    private record StatusInfo(DisclosureStatus disclosure, String disclosureReason) {
    }

    /**
     * 시간 범위를 담는 내부 레코드
     */
    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
