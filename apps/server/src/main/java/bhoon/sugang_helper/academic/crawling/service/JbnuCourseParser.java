package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseScheduleDto;
import bhoon.sugang_helper.academic.course.enums.CourseAccreditation;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SuppressWarnings("PMD.CyclomaticComplexity")
public class JbnuCourseParser {

    private static final Pattern PERIOD_TOKEN_PATTERN = Pattern.compile("^(\\d{1,2})-([ABab])$");
    private static final Pattern GRADE_IN_DEPT_PATTERN = Pattern.compile("\\s(?<grade>[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");
    private static final Pattern TRAILING_GRADE_PATTERN_TEMPLATE = Pattern
            .compile("^(?<before>.*?)(?:\\s+)(?<grade>[1-6])(?:학년)?(?:\\s*등)?$");
    private static final Pattern PERIOD_WITH_HALF_PATTERN = Pattern.compile("^(\\d{1,2})([ABab])$");
    private static final Pattern PERIOD_RANGE_WITH_HALF_PATTERN = Pattern.compile(
            "^(\\d{1,2})([ABab])\\s*[-~]\\s*(\\d{1,2})([ABab])$");
    private static final Pattern PERIOD_RANGE_PATTERN = Pattern.compile("^(\\d{1,2})\\s*[-~]\\s*(\\d{1,2})$");
    private static final Pattern CLOCK_RANGE_PATTERN = Pattern.compile(
            "^(\\d{1,2}:\\d{2})\\s*[-~]\\s*(\\d{1,2}:\\d{2})$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();




    public List<ParsedCourseDto> parseCourses(String jsonData) {
        return parseJumpCourses(jsonData, null, null);
    }

    public List<ParsedCourseDto> parseCourses(String jsonData, String year, String semester) {
        return parseJumpCourses(jsonData, year, semester);
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream) {
        return streamCourses(responseStream, null, null);
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream, String year, String semester) {
        try {
            String jsonData = new String(responseStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return parseJumpCourses(jsonData, year, semester).iterator();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "Malformed crawler JSON: " + e.getMessage());
        }
    }

    private List<ParsedCourseDto> parseJumpCourses(String jsonData, String fallbackYear, String fallbackSemester) {
        List<ParsedCourseDto> courseList = new ArrayList<>();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jsonData);
            if (root == null || !root.isObject()) {
                throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP response must be a JSON object");
            }
            if (root.hasNonNull("ERRMSGINFO")) {
                throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR, 
                        "JUMP returned an error envelope: " + root.get("ERRMSGINFO").toString());
            }
            JsonNode rows = root.get("dsEstSbjList");
            if (rows == null || !rows.isArray()) {
                throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP response is missing dsEstSbjList");
            }
            int rowIndex = 0;
            for (JsonNode row : rows) {
                try {
                    courseList.add(processJumpRow(row, fallbackYear, fallbackSemester));
                } catch (RuntimeException e) {
                    log.warn("Failed to parse JUMP course row. rowIndex={}, exceptionType={}", rowIndex,
                            SensitiveDataRedactor.exceptionType(e));
                    throw e;
                }
                rowIndex++;
            }
            return courseList;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "Malformed crawler JSON: " + e.getMessage());
        }
    }

    private ParsedCourseDto processJumpRow(JsonNode row, String fallbackYear, String fallbackSemester) {
        if (row == null || !row.isObject()) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP course row must be a JSON object");
        }
        String subjectCode = jsonValue(row, "SBJCT_CD");
        String classNumber = jsonValue(row, "DVCLS_NO");
        String year = fallbackYear != null && !fallbackYear.isBlank() ? fallbackYear : jsonValue(row, "YRSA");
        String semester = fallbackSemester != null && !fallbackSemester.isBlank()
                ? fallbackSemester
                : jsonValue(row, "SEMSTR_CD");
        if (subjectCode == null || classNumber == null || year == null || semester == null) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP course row is missing required identity fields");
        }
        String stdtrNo = normalizeCourseNumber(jsonValue(row, "STDTR_NO"));

        String targetGradeValue = jsonValue(row, "ATNLC_TRGT_DIVCDNM");
        String department = jsonValue(row, "MNG_SCSBJT_CDNM");
        TargetGrade targetGrade = parseTargetGrade(targetGradeValue,
                firstNonBlank(jsonValue(row, "SCSBJT_SCYR_NTC_CN"), department));
        String disclosureValue = jsonValue(row, "RLS_YN");
        DisclosureStatus disclosure = "Y".equalsIgnoreCase(disclosureValue) ? DisclosureStatus.PUBLIC
                : "N".equalsIgnoreCase(disclosureValue) ? DisclosureStatus.PRIVATE
                : DisclosureStatus.from(disclosureValue);
        String operation = firstNonBlank(jsonValue(row, "OPNLT_DIVCDNM"), jsonValue(row, "LESSN_OPER_DRC_CN"));
        int effectiveCapacity = safeParseInt(firstNonBlank(
                jsonValue(row, "PRM_NMPR_CNT"), jsonValue(row, "ATNLC_PSCP_CNT")));

        return new ParsedCourseDto(
                generateCourseKey(year, semester, subjectCode, classNumber),
                subjectCode,
                stdtrNo,
                normalizeSubjectName(jsonValue(row, "SBJCT_NM"), classNumber),
                classNumber,
                jsonValue(row, "STAFFNM"),
                effectiveCapacity,
                safeParseInt(jsonValue(row, "TLSN_RCNT")),
                targetGrade,
                year,
                semester,
                CourseClassification.from(jsonValue(row, "CMCRS_DIVCDNM")),
                normalizeDepartmentName(department, targetGrade),
                GradingMethod.from(jsonValue(row, "RLT_ABSLT_EVL_DIVCDNM")),
                jsonValue(row, "DOW_HR_CN"),
                jsonValue(row, "ESTBL_PNT"),
                LectureLanguage.from(jsonValue(row, "LCTR_LANG_DIVCDNM")),
                disclosure,
                jsonValue(row, "PRVT_DIVCDNM"),
                safeParseInt(jsonValue(row, "HR_CNT")),
                jsonValue(row, "RLM_DIVCDNM"),
                jsonValue(row, "CULT_RLM_DIVCDNM"),
                CourseAccreditation.from(jsonValue(row, "CERT_DIVCDNM")),
                CourseStatus.from(operation),
                jsonValue(row, "DONG_RMNM_CN"),
                "Y".equalsIgnoreCase(jsonValue(row, "SUB_PLAN_YN")),
                jsonValue(row, "RLM_DIVCDNM_HIST"),
                jsonValue(row, "LESSN_OPER_DRC_CN"),
                jsonValue(row, "LESSN_HR_DIVCDNM"),
                parseSchedules(jsonValue(row, "DOW_HR_CN")));
    }

    private String jsonValue(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return normalizeClassroom(value.asText());
    }

    private String normalizeCourseNumber(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }



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




    private String generateCourseKey(String year, String semester, String sbjtCd, String clss) {
        return String.format("%s:%s:%s:%s", year, semester, sbjtCd, clss);
    }




    private TargetGrade parseTargetGrade(String gradeNm, String deptNm) {
        TargetGrade grade = TargetGrade.from(gradeNm);


        if (grade != null) {
            return grade;
        }

        if (deptNm == null || deptNm.isBlank()) {
            return null;
        }


        if (deptNm.contains("계열")) {
            return TargetGrade.GRADE_1;
        }


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




    private String normalizeClassroom(String value) {
        if (value == null || value.isBlank() || value.equals(":")) {
            return null;
        }
        return value.trim();
    }

    private int safeParseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }




    private List<ParsedCourseScheduleDto> parseSchedules(String timeString) {
        List<ParsedCourseScheduleDto> schedules = new ArrayList<>();
        if (timeString == null || timeString.isBlank()) {
            return schedules;
        }

        String[] tokens = timeString.split(",");
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            ParsedCourseScheduleDto schedule = parseCourseSchedule(trimmedToken.split("\\s+"));
            if (schedule == null) {
                continue;
            }
            schedules.add(schedule);
        }
        return mergeConsecutiveSchedules(schedules);
    }

    private ParsedCourseScheduleDto parseCourseSchedule(String[] parts) {
        if (parts.length < 2) {
            return null;
        }

        CourseDayOfWeek day = CourseDayOfWeek.from(parts[0]);
        TimeRange timeRange = parseTimeRange(parts[1]);

        if (day == null || timeRange == null) {
            return null;
        }

        return new ParsedCourseScheduleDto(day, timeRange.start(), timeRange.end());
    }




    private List<ParsedCourseScheduleDto> mergeConsecutiveSchedules(List<ParsedCourseScheduleDto> schedules) {
        if (schedules.isEmpty()) {
            return schedules;
        }

        List<ParsedCourseScheduleDto> sorted = schedules.stream()
                .sorted(Comparator
                        .comparing((ParsedCourseScheduleDto schedule) -> schedule.dayOfWeek().ordinal())
                        .thenComparing(ParsedCourseScheduleDto::startTime))
                .toList();

        List<ParsedCourseScheduleDto> merged = new ArrayList<>();
        ParsedCourseScheduleDto current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            ParsedCourseScheduleDto next = sorted.get(i);
            if (current.dayOfWeek() == next.dayOfWeek()
                    && current.endTime().equals(next.startTime())) {
                current = new ParsedCourseScheduleDto(current.dayOfWeek(), current.startTime(), next.endTime());
                continue;
            }

            merged.add(current);
            current = next;
        }

        merged.add(current);
        return merged;
    }




    private TimeRange parseTimeRange(String periodToken) {
        if (periodToken == null) {
            return null;
        }

        Matcher matcher = PERIOD_TOKEN_PATTERN.matcher(periodToken.trim());
        String normalizedToken = periodToken.trim();
        if (matcher.matches()) {
            return periodTimeRange(Integer.parseInt(matcher.group(1)), matcher.group(2));
        }

        Matcher halfMatcher = PERIOD_WITH_HALF_PATTERN.matcher(normalizedToken);
        if (halfMatcher.matches()) {
            return periodTimeRange(Integer.parseInt(halfMatcher.group(1)), halfMatcher.group(2));
        }

        Matcher halfRangeMatcher = PERIOD_RANGE_WITH_HALF_PATTERN.matcher(normalizedToken);
        if (halfRangeMatcher.matches()) {
            TimeRange start = periodTimeRange(Integer.parseInt(halfRangeMatcher.group(1)), halfRangeMatcher.group(2));
            TimeRange end = periodTimeRange(Integer.parseInt(halfRangeMatcher.group(3)), halfRangeMatcher.group(4));
            return start == null || end == null ? null : new TimeRange(start.start(), end.end());
        }

        Matcher periodRangeMatcher = PERIOD_RANGE_PATTERN.matcher(normalizedToken);
        if (periodRangeMatcher.matches()) {
            TimeRange start = periodTimeRange(Integer.parseInt(periodRangeMatcher.group(1)), "A");
            TimeRange end = periodTimeRange(Integer.parseInt(periodRangeMatcher.group(2)), "B");
            return start == null || end == null ? null : new TimeRange(start.start(), end.end());
        }

        Matcher clockRangeMatcher = CLOCK_RANGE_PATTERN.matcher(normalizedToken);
        if (clockRangeMatcher.matches()) {
            try {
                return new TimeRange(LocalTime.parse(clockRangeMatcher.group(1)),
                        LocalTime.parse(clockRangeMatcher.group(2)));
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private TimeRange periodTimeRange(int slot, String halfValue) {
        if (slot < 0 || slot > 15) {
            return null;
        }

        String half = halfValue.toUpperCase();
        int hour = 8 + slot;


        if ("A".equals(half)) {
            return new TimeRange(LocalTime.of(hour, 0), LocalTime.of(hour, 30));
        }


        if (slot == 15) {
            return new TimeRange(LocalTime.of(23, 30), LocalTime.of(23, 59));
        }
        return new TimeRange(LocalTime.of(hour, 30), LocalTime.of(hour + 1, 0));
    }




    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
