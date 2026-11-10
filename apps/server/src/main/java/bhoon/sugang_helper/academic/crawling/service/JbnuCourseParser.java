package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.enums.CourseAccreditation;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JbnuCourseParser {

    private static final String ERROR_ENVELOPE_FIELD = "ERRMSGINFO";
    private static final String COURSE_LIST_FIELD = "dsEstSbjList";

    private static final String SUBJECT_CODE_FIELD = "SBJCT_CD";
    private static final String CLASS_NUMBER_FIELD = "DVCLS_NO";
    private static final String ACADEMIC_YEAR_FIELD = "YRSA";
    private static final String SEMESTER_FIELD = "SEMSTR_CD";
    private static final String COURSE_NUMBER_FIELD = "STDTR_NO";
    private static final String SUBJECT_NAME_FIELD = "SBJCT_NM";
    private static final String PROFESSOR_FIELD = "STAFFNM";
    private static final String CAPACITY_FIELD = "PRM_NMPR_CNT";
    private static final String FALLBACK_CAPACITY_FIELD = "ATNLC_PSCP_CNT";
    private static final String ENROLLED_COUNT_FIELD = "TLSN_RCNT";
    private static final String TARGET_GRADE_FIELD = "ATNLC_TRGT_DIVCDNM";
    private static final String TARGET_GRADE_NOTICE_FIELD = "SCSBJT_SCYR_NTC_CN";
    private static final String CLASSIFICATION_FIELD = "CMCRS_DIVCDNM";
    private static final String DEPARTMENT_FIELD = "MNG_SCSBJT_CDNM";
    private static final String GRADING_METHOD_FIELD = "RLT_ABSLT_EVL_DIVCDNM";
    private static final String CLASS_TIME_FIELD = "DOW_HR_CN";
    private static final String CREDITS_FIELD = "ESTBL_PNT";
    private static final String LECTURE_LANGUAGE_FIELD = "LCTR_LANG_DIVCDNM";
    private static final String DISCLOSURE_FIELD = "RLS_YN";
    private static final String DISCLOSURE_REASON_FIELD = "PRVT_DIVCDNM";
    private static final String LECTURE_HOURS_FIELD = "HR_CNT";
    private static final String GENERAL_CATEGORY_FIELD = "RLM_DIVCDNM";
    private static final String GENERAL_DETAIL_FIELD = "CULT_RLM_DIVCDNM";
    private static final String ACCREDITATION_FIELD = "CERT_DIVCDNM";
    private static final String COURSE_STATUS_FIELD = "OPNLT_DIVCDNM";
    private static final String CLASSROOM_FIELD = "DONG_RMNM_CN";
    private static final String SYLLABUS_FLAG_FIELD = "SUB_PLAN_YN";
    private static final String GENERAL_CATEGORY_BY_YEAR_FIELD = "RLM_DIVCDNM_HIST";
    private static final String COURSE_DIRECTION_FIELD = "LESSN_OPER_DRC_CN";
    private static final String CLASS_DURATION_FIELD = "LESSN_HR_DIVCDNM";

    private static final String YES_FLAG = "Y";
    private static final String NO_FLAG = "N";
    private static final String MISSING_VALUE_MARKER = ":";
    private static final String GRADE_ENUM_PREFIX = "GRADE_";
    private static final String GRADE_GROUP = "grade";
    private static final Pattern GRADE_IN_DEPT_PATTERN =
            Pattern.compile("\\s(?<" + GRADE_GROUP + ">[1-6])(?=[\\s,]|$)");
    private static final Pattern TRAILING_NUMBER_IN_SUBJECT_PATTERN = Pattern
            .compile("^(?<subjectName>.*?)(?:\\s+)(?<number>\\d+)$");
    private static final Pattern TRAILING_GRADE_PATTERN = Pattern
            .compile("^(?<before>.*?)(?:\\s+)(?<" + GRADE_GROUP + ">[1-6])(?:학년)?(?:\\s*등)?$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<ParsedCourseDto> parseCourses(String jsonData) {
        return parseCourses(jsonData, null, null);
    }

    public List<ParsedCourseDto> parseCourses(String jsonData, String year, String semester) {
        JsonNode rows = readCourseRows(jsonData);
        List<ParsedCourseDto> courses = new ArrayList<>(rows.size());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            try {
                courses.add(parseCourse(rows.get(rowIndex), year, semester));
            } catch (RuntimeException exception) {
                logCourseRowParseFailed(rowIndex, exception);
                throw exception;
            }
        }
        return courses;
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream) {
        return streamCourses(responseStream, null, null);
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream, String year, String semester) {
        try {
            String jsonData = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            return parseCourses(jsonData, year, semester).iterator();
        } catch (IOException exception) {
            throw malformedJson(exception);
        }
    }

    private JsonNode readCourseRows(String jsonData) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(jsonData);
        } catch (IOException exception) {
            throw malformedJson(exception);
        }

        if (root == null || !root.isObject()) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP response must be a JSON object");
        }
        if (root.hasNonNull(ERROR_ENVELOPE_FIELD)) {
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR,
                    "JUMP returned an error envelope: " + root.get(ERROR_ENVELOPE_FIELD));
        }
        JsonNode rows = root.path(COURSE_LIST_FIELD);
        if (!rows.isArray()) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP response is missing " + COURSE_LIST_FIELD);
        }
        return rows;
    }

    private CustomException malformedJson(IOException exception) {
        return new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "Malformed crawler JSON: " + exception.getMessage());
    }

    private ParsedCourseDto parseCourse(JsonNode row, String requestedYear, String requestedSemester) {
        if (row == null || !row.isObject()) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP course row must be a JSON object");
        }
        String subjectCode = jsonValue(row, SUBJECT_CODE_FIELD);
        String classNumber = jsonValue(row, CLASS_NUMBER_FIELD);
        String year = firstNonBlank(requestedYear, jsonValue(row, ACADEMIC_YEAR_FIELD));
        String semester = firstNonBlank(requestedSemester, jsonValue(row, SEMESTER_FIELD));
        if (subjectCode == null || classNumber == null || year == null || semester == null) {
            throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, "JUMP course row is missing required identity fields");
        }
        String stdtrNo = normalizeCourseNumber(jsonValue(row, COURSE_NUMBER_FIELD));

        String targetGradeValue = jsonValue(row, TARGET_GRADE_FIELD);
        String department = jsonValue(row, DEPARTMENT_FIELD);
        TargetGrade targetGrade = parseTargetGrade(targetGradeValue,
                firstNonBlank(jsonValue(row, TARGET_GRADE_NOTICE_FIELD), department));
        String courseDirection = jsonValue(row, COURSE_DIRECTION_FIELD);
        String operation = firstNonBlank(jsonValue(row, COURSE_STATUS_FIELD), courseDirection);
        String classTime = jsonValue(row, CLASS_TIME_FIELD);
        int effectiveCapacity = parseIntOrZero(firstNonBlank(
                jsonValue(row, CAPACITY_FIELD), jsonValue(row, FALLBACK_CAPACITY_FIELD)));

        return new ParsedCourseDto(
                generateCourseKey(year, semester, subjectCode, classNumber),
                subjectCode,
                stdtrNo,
                normalizeSubjectName(jsonValue(row, SUBJECT_NAME_FIELD), classNumber),
                classNumber,
                jsonValue(row, PROFESSOR_FIELD),
                effectiveCapacity,
                parseIntOrZero(jsonValue(row, ENROLLED_COUNT_FIELD)),
                targetGrade,
                year,
                semester,
                CourseClassification.from(jsonValue(row, CLASSIFICATION_FIELD)),
                normalizeDepartmentName(department, targetGrade),
                GradingMethod.from(jsonValue(row, GRADING_METHOD_FIELD)),
                classTime,
                jsonValue(row, CREDITS_FIELD),
                LectureLanguage.from(jsonValue(row, LECTURE_LANGUAGE_FIELD)),
                parseDisclosure(jsonValue(row, DISCLOSURE_FIELD)),
                jsonValue(row, DISCLOSURE_REASON_FIELD),
                parseIntOrZero(jsonValue(row, LECTURE_HOURS_FIELD)),
                jsonValue(row, GENERAL_CATEGORY_FIELD),
                jsonValue(row, GENERAL_DETAIL_FIELD),
                CourseAccreditation.from(jsonValue(row, ACCREDITATION_FIELD)),
                CourseStatus.from(operation),
                jsonValue(row, CLASSROOM_FIELD),
                YES_FLAG.equalsIgnoreCase(jsonValue(row, SYLLABUS_FLAG_FIELD)),
                jsonValue(row, GENERAL_CATEGORY_BY_YEAR_FIELD),
                courseDirection,
                jsonValue(row, CLASS_DURATION_FIELD),
                JbnuCourseScheduleParser.parse(classTime));
    }

    private DisclosureStatus parseDisclosure(String value) {
        if (YES_FLAG.equalsIgnoreCase(value)) {
            return DisclosureStatus.PUBLIC;
        }
        if (NO_FLAG.equalsIgnoreCase(value)) {
            return DisclosureStatus.PRIVATE;
        }
        return DisclosureStatus.from(value);
    }

    private String jsonValue(JsonNode row, String field) {
        String text = row.path(field).asText(null);
        if (text == null || text.isBlank() || text.equals(MISSING_VALUE_MARKER)) {
            return null;
        }
        return text.trim();
    }

    private String normalizeCourseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        return first;
    }

    private String normalizeSubjectName(String rawSubjectName, String classNumber) {
        if (rawSubjectName == null || rawSubjectName.isBlank()) {
            return rawSubjectName;
        }

        if (classNumber.isBlank()) {
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
            Matcher matcher = TRAILING_GRADE_PATTERN.matcher(current);
            if (!matcher.matches()) {
                return current.trim();
            }

            String suffixGrade = matcher.group(GRADE_GROUP);
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
        if (targetGrade == null) {
            return null;
        }

        String name = targetGrade.name();
        if (name.startsWith(GRADE_ENUM_PREFIX) && name.length() == GRADE_ENUM_PREFIX.length() + 1) {
            return name.substring(GRADE_ENUM_PREFIX.length());
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
            lastMatchedGrade = matcher.group(GRADE_GROUP);
        }

        return TargetGrade.from(lastMatchedGrade);
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void logCourseRowParseFailed(int rowIndex, RuntimeException exception) {
        log.atWarn().addKeyValue("event", "crawler_course_row_parse_failed")
                .addKeyValue("rowIndex", rowIndex)
                .addKeyValue("exceptionType", SensitiveDataRedactor.exceptionType(exception))
                .log("JUMP 강의 행 파싱 실패.");
    }
}
