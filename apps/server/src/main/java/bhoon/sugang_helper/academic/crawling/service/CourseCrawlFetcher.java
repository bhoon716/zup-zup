package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourse;
import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseSchedule;
import bhoon.sugang_helper.academic.crawling.client.CourseApiClient;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlFetcher {

    private static final String COURSE_API_STAGE = "COURSE_API";
    private static final String CERT_DIVISION_KEY = "certDiv";
    private static final String EVENT_KEY = "event";
    private static final String RESULT_KEY = "result";
    private static final String SEMESTER_KEY = "semester";
    private static final String STAGE_KEY = "stage";
    private static final String YEAR_KEY = "year";

    private final CourseApiClient apiClient;
    private final JbnuCourseParser courseParser;

    @Value("${jbnu.api.cert-divisions}")
    private List<String> certDivisions;

    @Value("${jbnu.api.inter-request-delay-ms}")
    private long interRequestDelayMs;

    public List<ParsedCourse> fetch(String year, String semester) {
        List<ParsedCourse> allCourses = fetchAllChunks(year, semester, certDivisions);

        validateCompleteResult(allCourses);
        return List.copyOf(allCourses);
    }

    private List<ParsedCourse> fetchAllChunks(String year, String semester, List<String> divisions) {
        List<ParsedCourse> accumulated = new ArrayList<>();
        int totalDivisions = divisions.size();

        try (CourseApiClient.Session session = apiClient.openSession()) {
            for (int i = 0; i < totalDivisions; i++) {
                String certDiv = divisions.get(i);
                fetchSingleChunk(session, year, semester, certDiv, accumulated);
                paceRequest(i, totalDivisions);
            }
        }
        return accumulated;
    }

    private void fetchSingleChunk(CourseApiClient.Session session, String year, String semester, String certDiv,
                                  List<ParsedCourse> accumulator) {
        long startNs = System.nanoTime();
        logChunkStarted(year, semester, certDiv);

        try (InputStream responseStream = session.fetchCourseDataStream(year, semester, certDiv)) {
            Iterator<ParsedCourse> iterator = courseParser.streamCourses(responseStream, year, semester);
            int initialSize = accumulator.size();
            iterator.forEachRemaining(accumulator::add);

            logChunkSucceeded(year, semester, certDiv, accumulator.size() - initialSize, accumulator.size(), startNs);
        } catch (IOException exception) {
            logChunkFailed(year, semester, certDiv, exception);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    private void logChunkStarted(String year, String semester, String certDiv) {
        log.atDebug().addKeyValue(EVENT_KEY, "chunk_started")
                .addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester)
                .addKeyValue(CERT_DIVISION_KEY, certDiv)
                .addKeyValue(STAGE_KEY, COURSE_API_STAGE)
                .addKeyValue(RESULT_KEY, "started")
                .log("[Crawler] 강의 묶음 수집 시작.");
    }

    private void logChunkSucceeded(String year, String semester, String certDiv, int courseCount,
                                    int totalCount, long startNs) {
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        log.atDebug().addKeyValue(EVENT_KEY, "chunk_succeeded")
                .addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester)
                .addKeyValue(CERT_DIVISION_KEY, certDiv)
                .addKeyValue("rowCount", courseCount)
                .addKeyValue("totalSoFar", totalCount)
                .addKeyValue("elapsedMs", elapsedMs)
                .addKeyValue(STAGE_KEY, COURSE_API_STAGE)
                .addKeyValue(RESULT_KEY, "success")
                .log("[Crawler] 강의 묶음 수집 완료.");
    }

    private void logChunkFailed(String year, String semester, String certDiv, IOException exception) {
        log.atError().addKeyValue(EVENT_KEY, "chunk_failed")
                .addKeyValue(YEAR_KEY, year)
                .addKeyValue(SEMESTER_KEY, semester)
                .addKeyValue(CERT_DIVISION_KEY, certDiv)
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .addKeyValue(STAGE_KEY, COURSE_API_STAGE)
                .addKeyValue(RESULT_KEY, "failed")
                .addKeyValue("failureCode", "C001")
                .addKeyValue("retryable", true)
                .log("[Crawler] 강의 묶음 수집 실패.");
    }

    private void paceRequest(int currentIndex, int totalCount) {
        if (interRequestDelayMs <= 0 || currentIndex >= totalCount - 1) {
            return;
        }
        try {
            Thread.sleep(interRequestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "크롤링 파트 대기 중 중단되었습니다.");
        }
    }

    private void validateCompleteResult(List<ParsedCourse> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA);
        }

        Set<String> courseKeys = new HashSet<>();
        for (ParsedCourse course : courses) {
            validateCourseEntry(course, courseKeys);
        }
    }

    private void validateCourseEntry(ParsedCourse course, Set<String> courseKeys) {
        validateRequiredFields(course);
        String canonicalCourseKey = course.courseKey().trim();
        if (!courseKeys.add(canonicalCourseKey)) {
            throwParsingError("수집 결과에 중복된 강의 키가 있습니다.");
        }
        validateSchedules(course);
    }

    private void validateRequiredFields(ParsedCourse course) {
        if (course == null
                || !StringUtils.hasText(course.courseKey())
                || !StringUtils.hasText(course.subjectCode())
                || !StringUtils.hasText(course.name())
                || !StringUtils.hasText(course.classNumber())
                || !StringUtils.hasText(course.academicYear())
                || !StringUtils.hasText(course.semester())
                || course.capacity() == null
                || course.current() == null) {
            throwParsingError("수집 결과에 강의 필수 항목이 누락됐습니다.");
        }
    }

    private void validateSchedules(ParsedCourse course) {
        if (course.schedules() == null) {
            return;
        }
        for (ParsedCourseSchedule schedule : course.schedules()) {
            if (schedule == null
                    || schedule.dayOfWeek() == null
                    || schedule.startTime() == null
                    || schedule.endTime() == null) {
                throwParsingError("수집 결과에 필수 값이 누락된 강의 시간표가 있습니다.");
            }
        }
    }

    private void throwParsingError(String detail) {
        throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, detail);
    }
}
