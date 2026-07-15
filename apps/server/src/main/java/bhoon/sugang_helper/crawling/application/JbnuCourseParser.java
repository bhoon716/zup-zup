package bhoon.sugang_helper.crawling.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
import bhoon.sugang_helper.course.domain.CourseAccreditation;
import bhoon.sugang_helper.course.domain.CourseClassification;
import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.CourseStatus;
import bhoon.sugang_helper.course.domain.DisclosureStatus;
import bhoon.sugang_helper.course.domain.GradingMethod;
import bhoon.sugang_helper.course.domain.LectureLanguage;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.TargetGrade;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@Slf4j
@Component
@SuppressWarnings("PMD.CyclomaticComplexity") // Supports both legacy DOM parsing and the bounded streaming path.
public class JbnuCourseParser {

    private static final String DATASET_ID = "GRD_COUR001";
    private static final String CLASS_TIME_COLUMN = "DAYTMCTNT";
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

    /**
     * XML 데이터를 파싱하여 ParsedCourseDto 리스트로 변환합니다.
     */
    public List<ParsedCourseDto> parseCourses(String xmlData) {
        if (looksLikeJson(xmlData)) {
            return parseJumpCourses(xmlData, null, null);
        }
        List<ParsedCourseDto> courseList = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", Parser.xmlParser());

        Elements rows = doc.select("Dataset[id=" + DATASET_ID + "] > Rows > Row");

        for (Element row : rows) {
            try {
                processRow(row).ifPresent(courseList::add);
            } catch (Exception e) {
                log.warn("Failed to parse course row. exceptionType={}", SensitiveDataRedactor.exceptionType(e));
            }
        }
        return courseList;
    }

    public List<ParsedCourseDto> parseCourses(String data, String year, String semester) {
        if (looksLikeJson(data)) {
            return parseJumpCourses(data, year, semester);
        }
        return parseCourses(data);
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream) {
        return streamCourses(responseStream, null, null);
    }

    public Iterator<ParsedCourseDto> streamCourses(InputStream responseStream, String year, String semester) {
        BufferedInputStream bufferedStream = responseStream instanceof BufferedInputStream
                ? (BufferedInputStream) responseStream
                : new BufferedInputStream(responseStream);
        try {
            bufferedStream.mark(8192);
            int firstByte;
            do {
                firstByte = bufferedStream.read();
            } while (firstByte >= 0 && Character.isWhitespace(firstByte));
            bufferedStream.reset();
            if (firstByte == '{' || firstByte == '[') {
                return parseJumpCourses(new String(bufferedStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
                        year, semester).iterator();
            }
            return streamXmlCourses(bufferedStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed crawler JSON", e);
        }
    }

    private Iterator<ParsedCourseDto> streamXmlCourses(InputStream xmlStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            return new StaxCourseIterator(factory.createXMLStreamReader(xmlStream));
        } catch (XMLStreamException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed crawler XML", e);
        }
    }

    private List<ParsedCourseDto> parseJumpCourses(String jsonData, String fallbackYear, String fallbackSemester) {
        List<ParsedCourseDto> courseList = new ArrayList<>();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jsonData);
            if (root.hasNonNull("ERRMSGINFO")) {
                throw new IllegalArgumentException("JUMP returned an error envelope");
            }
            JsonNode rows = root.isArray() ? root : root.path("dsEstSbjList");
            if (!rows.isArray()) {
                return courseList;
            }
            for (JsonNode row : rows) {
                try {
                    processJumpRow(row, fallbackYear, fallbackSemester).ifPresent(courseList::add);
                } catch (Exception e) {
                    log.warn("Failed to parse JUMP course row. exceptionType={}", SensitiveDataRedactor.exceptionType(e));
                }
            }
            return courseList;
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed crawler JSON", e);
        }
    }

    private Optional<ParsedCourseDto> processJumpRow(JsonNode row, String fallbackYear, String fallbackSemester) {
        String subjectCode = jsonValue(row, "SBJCT_CD");
        String classNumber = jsonValue(row, "DVCLS_NO");
        String year = fallbackYear != null && !fallbackYear.isBlank() ? fallbackYear : jsonValue(row, "YRSA");
        String semester = fallbackSemester != null && !fallbackSemester.isBlank()
                ? fallbackSemester
                : jsonValue(row, "SEMSTR_CD");
        if (subjectCode == null || classNumber == null || year == null || semester == null) {
            return Optional.empty();
        }

        String targetGradeValue = jsonValue(row, "ATNLC_TRGT_DIVCDNM");
        String department = jsonValue(row, "MNG_SCSBJT_CDNM");
        TargetGrade targetGrade = parseTargetGrade(targetGradeValue,
                firstNonBlank(jsonValue(row, "SCSBJT_SCYR_NTC_CN"), department));
        String disclosureValue = jsonValue(row, "RLS_YN");
        DisclosureStatus disclosure = "Y".equalsIgnoreCase(disclosureValue) ? DisclosureStatus.PUBLIC
                : "N".equalsIgnoreCase(disclosureValue) ? DisclosureStatus.PRIVATE
                : DisclosureStatus.from(disclosureValue);
        String operation = firstNonBlank(jsonValue(row, "OPNLT_DIVCDNM"), jsonValue(row, "LESSN_OPER_DRC_CN"));

        return Optional.of(new ParsedCourseDto(
                generateCourseKey(year, semester, subjectCode, classNumber),
                subjectCode,
                normalizeSubjectName(jsonValue(row, "SBJCT_NM"), classNumber),
                classNumber,
                jsonValue(row, "STAFFNM"),
                safeParseInt(firstNonBlank(jsonValue(row, "ATNLC_PSCP_CNT"), jsonValue(row, "PRM_NMPR_CNT"))),
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
                parseSchedules(jsonValue(row, "DOW_HR_CN"))));
    }

    private String jsonValue(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return normalizeClassroom(value.asText());
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean looksLikeJson(String value) {
        return value != null && !value.stripLeading().isEmpty()
                && (value.stripLeading().charAt(0) == '{' || value.stripLeading().charAt(0) == '[');
    }

    private Optional<ParsedCourseDto> processColumns(Map<String, String> columns) {
        String sbjtCd = columns.get("SBJTCD");
        String clss = columns.get("CLSS");
        String year = columns.get("YY");
        String semester = columns.get("SHTM");
        String rawDepartment = columns.get("SUSTCDNM");
        String gradeNm = columns.get("TLSNOBJFGNM");

        if (sbjtCd == null || clss == null || year == null || semester == null) {
            return Optional.empty();
        }

        LiberalArtsInfo liberalArts = new LiberalArtsInfo(columns.get("FLDFGNM"), columns.get("FLDDETAFGNM"));
        StatusInfo statusInfo = new StatusInfo(DisclosureStatus.from(columns.get("PUBCYN")),
                columns.get("NOPUBCRESNNM"));
        TargetGrade targetGrade = parseTargetGrade(gradeNm, rawDepartment);
        String subjectName = normalizeSubjectName(columns.get("SBJTNM"), clss);
        String department = normalizeDepartmentName(rawDepartment, targetGrade);

        return Optional.of(new ParsedCourseDto(
                generateCourseKey(year, semester, sbjtCd, clss), sbjtCd, subjectName, clss,
                columns.get("RPSTPROFNM"), safeParseInt(columns.get("LMTRCNT")),
                safeParseInt(columns.get("TLSNRCNT")), targetGrade, year, semester,
                CourseClassification.from(columns.get("CPTNFGNM")), department,
                GradingMethod.from(columns.get("SCORTRETFGNM")), columns.get(CLASS_TIME_COLUMN),
                columns.get("PNT"), LectureLanguage.from(columns.get("LTLANGFGNM")), statusInfo.disclosure,
                statusInfo.disclosureReason, safeParseInt(columns.get("TM")), liberalArts.category,
                liberalArts.detail, CourseAccreditation.from(columns.get("VLDFGNM")),
                CourseStatus.from(columns.get("OPENLECTFGNM")), columns.get("VILROOMNOCTNT"),
                "Y".equalsIgnoreCase(columns.get("SUBPLANYN")), columns.get("FLDCONVINFO"),
                columns.get("CLSSOPRTDRCT"), columns.get("LESSTMFGNM"),
                parseSchedules(columns.get(CLASS_TIME_COLUMN))));
    }

    private final class StaxCourseIterator implements Iterator<ParsedCourseDto> {

        private final XMLStreamReader reader;
        private ParsedCourseDto next;
        private boolean inDataset;

        private StaxCourseIterator(XMLStreamReader reader) {
            this.reader = reader;
            advance();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ParsedCourseDto next() {
            ParsedCourseDto current = next;
            advance();
            return current;
        }

        @SuppressWarnings("PMD.NullAssignment")
        private void advance() {
            next = null;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamReader.START_ELEMENT && "Dataset".equals(reader.getLocalName())) {
                        inDataset = DATASET_ID.equals(reader.getAttributeValue(null, "id"));
                    } else if (event == XMLStreamReader.END_ELEMENT && "Dataset".equals(reader.getLocalName())) {
                        inDataset = false;
                    } else if (inDataset && event == XMLStreamReader.START_ELEMENT
                            && "Row".equals(reader.getLocalName())) {
                        Optional<ParsedCourseDto> parsed = processColumns(readRow());
                        if (parsed.isPresent()) {
                            next = parsed.get();
                            return;
                        }
                    }
                }
                if (inDataset) {
                    throw new XMLStreamException("Unexpected end of crawler XML dataset");
                }
                reader.close();
            } catch (XMLStreamException e) {
                throw new IllegalArgumentException("Malformed crawler XML", e);
            }
        }

        private Map<String, String> readRow() throws XMLStreamException {
            Map<String, String> columns = new HashMap<>();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT && "Col".equals(reader.getLocalName())) {
                    String id = reader.getAttributeValue(null, "id");
                    if (id != null) {
                        columns.put(id, normalizeClassroom(reader.getElementText().trim()));
                    }
                } else if (event == XMLStreamReader.END_ELEMENT && "Row".equals(reader.getLocalName())) {
                    return columns;
                }
            }
            throw new XMLStreamException("Unexpected end of crawler XML row");
        }
    }

    /**
     * 단일 행(Row) 데이터를 파싱하여 ParsedCourseDto 객체를 생성합니다.
     */
    private Optional<ParsedCourseDto> processRow(Element row) {
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

        return Optional.of(new ParsedCourseDto(
                generateCourseKey(year, semester, sbjtCd, clss),
                sbjtCd,
                subjectName,
                clss,
                getColValue(row, "RPSTPROFNM"),
                safeParseInt(getColValue(row, "LMTRCNT")),
                safeParseInt(getColValue(row, "TLSNRCNT")),
                targetGrade,
                year,
                semester,
                CourseClassification.from(getColValue(row, "CPTNFGNM")),
                department,
                GradingMethod.from(getColValue(row, "SCORTRETFGNM")),
                getColValue(row, CLASS_TIME_COLUMN),
                getColValue(row, "PNT"),
                LectureLanguage.from(getColValue(row, "LTLANGFGNM")),
                statusInfo.disclosure,
                statusInfo.disclosureReason,
                safeParseInt(getColValue(row, "TM")),
                liberalArts.category,
                liberalArts.detail,
                CourseAccreditation.from(getColValue(row, "VLDFGNM")),
                CourseStatus.from(getColValue(row, "OPENLECTFGNM")),
                getColValue(row, "VILROOMNOCTNT"),
                "Y".equalsIgnoreCase(getColValue(row, "SUBPLANYN")),
                getColValue(row, "FLDCONVINFO"),
                getColValue(row, "CLSSOPRTDRCT"),
                getColValue(row, "LESSTMFGNM"),
                parseSchedules(getColValue(row, CLASS_TIME_COLUMN))
        ));
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
        return col != null ? normalizeClassroom(col.text().trim()) : null;
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

    /**
     * 시간 정보 문자열을 파싱하여 스케줄 DTO 리스트를 생성합니다.
     */
    private List<ParsedCourseDto.ScheduleDto> parseSchedules(String timeString) {
        List<ParsedCourseDto.ScheduleDto> schedules = new ArrayList<>();
        if (timeString == null || timeString.isBlank()) {
            return schedules;
        }

        String[] tokens = timeString.split(",");
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            ParsedCourseDto.ScheduleDto schedule = parseCourseSchedule(trimmedToken.split("\\s+"));
            if (schedule == null) {
                continue;
            }
            schedules.add(schedule);
        }
        return mergeConsecutiveSchedules(schedules);
    }

    private ParsedCourseDto.ScheduleDto parseCourseSchedule(String[] parts) {
        if (parts.length < 2) {
            return null;
        }

        CourseDayOfWeek day = CourseDayOfWeek.from(parts[0]);
        TimeRange timeRange = parseTimeRange(parts[1]);

        if (day == null || timeRange == null) {
            return null;
        }

        return new ParsedCourseDto.ScheduleDto(day, timeRange.start(), timeRange.end());
    }

    /**
     * 연속된 시간대 정보를 하나의 시간대로 병합합니다. (예: 1A, 1B -> 1시간)
     */
    private List<ParsedCourseDto.ScheduleDto> mergeConsecutiveSchedules(List<ParsedCourseDto.ScheduleDto> schedules) {
        if (schedules.isEmpty()) {
            return schedules;
        }

        List<ParsedCourseDto.ScheduleDto> sorted = schedules.stream()
                .sorted(Comparator
                        .comparing((ParsedCourseDto.ScheduleDto schedule) -> schedule.dayOfWeek().ordinal())
                        .thenComparing(ParsedCourseDto.ScheduleDto::startTime))
                .toList();

        List<ParsedCourseDto.ScheduleDto> merged = new ArrayList<>();
        ParsedCourseDto.ScheduleDto current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            ParsedCourseDto.ScheduleDto next = sorted.get(i);
            if (current.dayOfWeek() == next.dayOfWeek()
                    && current.endTime().equals(next.startTime())) {
                current = new ParsedCourseDto.ScheduleDto(current.dayOfWeek(), current.startTime(), next.endTime());
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
