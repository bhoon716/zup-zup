package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
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

    private final JbnuCourseApiClient apiClient;
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

        for (int i = 0; i < totalDivisions; i++) {
            String certDiv = divisions.get(i);
            fetchSingleChunk(year, semester, certDiv, accumulated);
            paceRequest(i, totalDivisions);
        }
        return accumulated;
    }

    private void fetchSingleChunk(String year, String semester, String certDiv, List<ParsedCourseDto> accumulator) {
        long startNs = System.nanoTime();
        log.info("[CrawlFetcher] Fetching chunk for certDiv={}. year={}, semester={}", certDiv, year, semester);

        try (InputStream responseStream = apiClient.fetchCourseDataStream(year, semester, certDiv)) {
            Iterator<ParsedCourseDto> iterator = courseParser.streamCourses(responseStream, year, semester);
            int initialSize = accumulator.size();
            iterator.forEachRemaining(accumulator::add);

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("[CrawlFetcher] Fetched chunk certDiv={}. rowCount={}, totalSoFar={}, elapsedMs={}",
                    certDiv, accumulator.size() - initialSize, accumulator.size(), elapsedMs);
        } catch (IOException exception) {
            log.error("[CrawlFetcher] Failed to fetch chunk certDiv={}. year={}, semester={}", certDiv, year, semester);
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
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
        for (ParsedCourseDto.ScheduleDto schedule : course.schedules()) {
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
