package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseScheduleDto;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlFetcher {

    private final CourseApiClient apiClient;
    private final JbnuCourseParser courseParser;

    @Value("${jbnu.api.cert-divisions}")
    private List<String> certDivisions;

    @Value("${jbnu.api.inter-request-delay-ms}")
    private long interRequestDelayMs;

    public List<ParsedCourseDto> fetch(String year, String semester) {
        List<ParsedCourseDto> allCourses = fetchAllChunks(year, semester, certDivisions);

        validateCompleteResult(allCourses);
        return List.copyOf(allCourses);
    }

    private List<ParsedCourseDto> fetchAllChunks(String year, String semester, List<String> divisions) {
        List<ParsedCourseDto> accumulated = new ArrayList<>();
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
                                  List<ParsedCourseDto> accumulator) {
        long startNs = System.nanoTime();
        logChunkStarted(year, semester, certDiv);

        try (InputStream responseStream = session.fetchCourseDataStream(year, semester, certDiv)) {
            Iterator<ParsedCourseDto> iterator = courseParser.streamCourses(responseStream, year, semester);
            int initialSize = accumulator.size();
            iterator.forEachRemaining(accumulator::add);

            logChunkSucceeded(year, semester, certDiv, accumulator.size() - initialSize, accumulator.size(), startNs);
        } catch (IOException exception) {
            logChunkFailed(year, semester, certDiv, exception);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    private void logChunkStarted(String year, String semester, String certDiv) {
        log.atDebug().addKeyValue("event", "chunk_started")
                .addKeyValue("year", year)
                .addKeyValue("semester", semester)
                .addKeyValue("certDiv", certDiv)
                .addKeyValue("stage", "COURSE_API")
                .addKeyValue("result", "started")
                .log("[Crawler] 강의 묶음 수집 시작.");
    }

    private void logChunkSucceeded(String year, String semester, String certDiv, int courseCount,
                                    int totalCount, long startNs) {
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        log.atDebug().addKeyValue("event", "chunk_succeeded")
                .addKeyValue("year", year)
                .addKeyValue("semester", semester)
                .addKeyValue("certDiv", certDiv)
                .addKeyValue("rowCount", courseCount)
                .addKeyValue("totalSoFar", totalCount)
                .addKeyValue("elapsedMs", elapsedMs)
                .addKeyValue("stage", "COURSE_API")
                .addKeyValue("result", "success")
                .log("[Crawler] 강의 묶음 수집 완료.");
    }

    private void logChunkFailed(String year, String semester, String certDiv, IOException exception) {
        log.atError().addKeyValue("event", "chunk_failed")
                .addKeyValue("year", year)
                .addKeyValue("semester", semester)
                .addKeyValue("certDiv", certDiv)
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .addKeyValue("stage", "COURSE_API")
                .addKeyValue("result", "failed")
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

    private void validateCompleteResult(List<ParsedCourseDto> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA);
        }

        Set<String> courseKeys = new HashSet<>();
        for (ParsedCourseDto course : courses) {
            validateCourseEntry(course, courseKeys);
        }
    }

    private void validateCourseEntry(ParsedCourseDto course, Set<String> courseKeys) {
        validateRequiredFields(course);
        String canonicalCourseKey = course.courseKey().trim();
        if (!courseKeys.add(canonicalCourseKey)) {
            throwParsingError("Crawler result contains a duplicate course key");
        }
        validateSchedules(course);
    }

    private void validateRequiredFields(ParsedCourseDto course) {
        if (course == null
                || isBlank(course.courseKey())
                || isBlank(course.subjectCode())
                || isBlank(course.name())
                || isBlank(course.classNumber())
                || isBlank(course.academicYear())
                || isBlank(course.semester())
                || course.capacity() == null
                || course.current() == null) {
            throwParsingError("Crawler result contains a missing required course field");
        }
    }

    private void validateSchedules(ParsedCourseDto course) {
        if (course.schedules() == null) {
            return;
        }
        for (ParsedCourseScheduleDto schedule : course.schedules()) {
            if (schedule == null
                    || schedule.dayOfWeek() == null
                    || schedule.startTime() == null
                    || schedule.endTime() == null) {
                throwParsingError("Crawler result contains an incomplete course schedule");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void throwParsingError(String detail) {
        throw new CustomException(ErrorCode.CRAWLER_PARSING_ERROR, detail);
    }
}
