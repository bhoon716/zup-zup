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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseCrawlFetcher {

    private final JbnuCourseApiClient apiClient;
    private final JbnuCourseParser courseParser;

    public List<ParsedCourseDto> fetch(String year, String semester) {
        try (InputStream responseStream = apiClient.fetchCourseDataStream(year, semester)) {
            Iterator<ParsedCourseDto> iterator = courseParser.streamCourses(responseStream, year, semester);
            List<ParsedCourseDto> courses = new ArrayList<>();
            iterator.forEachRemaining(courses::add);
            validateCompleteResult(courses);
            return List.copyOf(courses);
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        }
    }

    private void validateCompleteResult(List<ParsedCourseDto> courses) {
        if (courses.isEmpty()) {
            throw new CustomException(ErrorCode.CRAWLER_NO_DATA);
        }
        Set<String> courseKeys = new HashSet<>();
        for (ParsedCourseDto course : courses) {
            validateRequiredFields(course);
            String canonicalCourseKey = course.courseKey().trim();
            if (!courseKeys.add(canonicalCourseKey)) {
                throwParsingError("Crawler result contains a duplicate course key");
            }
            validateSchedules(course);
        }
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
