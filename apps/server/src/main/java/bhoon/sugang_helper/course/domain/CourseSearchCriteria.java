package bhoon.sugang_helper.course.domain;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;

@Builder
public record CourseSearchCriteria(
        String name,
        String keyword,
        String professor,
        String subjectCode,
        String academicYear,
        String semester,
        List<String> classifications,
        String department,
        List<String> gradingMethods,
        List<String> lectureLanguages,
        Boolean isAvailableOnly,
        String dayOfWeek,
        List<SelectedSchedule> selectedSchedules,
        List<String> credits,
        Integer lectureHours,
        Integer minLectureHours,
        String generalCategory,
        String generalDetail,
        Long timetableId,
        Boolean isWishedOnly,
        List<String> statuses,
        List<String> courseDirectionTerms,
        Double minCredits,
        List<TargetGrade> targetGrades,
        String disclosure,
        String sortBy,
        String sortOrder,
        Long userId
) {
    public static final int MAX_STATUS_FILTERS = 20;
    public static final int MAX_STATUS_LENGTH = 50;
    public static final int MAX_COURSE_DIRECTION_LENGTH = 500;
    public static final int MAX_COURSE_DIRECTION_TERMS = 10;
    public static final int MAX_COURSE_DIRECTION_TERM_LENGTH = 100;

    public CourseSearchCriteria {
        statuses = normalizeStatuses(statuses);
        courseDirectionTerms = normalizeDirectionTerms(courseDirectionTerms);
    }

    public static List<String> normalizeCourseDirectionTerms(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return normalizeDirectionTerms(Arrays.asList(rawValue.split("[,\\n;]+")));
    }

    private static List<String> normalizeStatuses(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> normalized = values.stream()
                .map(CourseSearchCriteria::trimRequired)
                .distinct()
                .toList();
        if (normalized.size() > MAX_STATUS_FILTERS
                || normalized.stream().anyMatch(value -> value.length() > MAX_STATUS_LENGTH)) {
            throw new IllegalArgumentException("statuses exceed the search predicate limit");
        }
        return normalized;
    }

    private static List<String> normalizeDirectionTerms(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> normalized = values.stream()
                .map(CourseSearchCriteria::trimRequired)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        if (normalized.size() > MAX_COURSE_DIRECTION_TERMS
                || normalized.stream().anyMatch(value -> value.length() > MAX_COURSE_DIRECTION_TERM_LENGTH)) {
            throw new IllegalArgumentException("courseDirection terms exceed the search predicate limit");
        }
        return normalized;
    }

    private static String trimRequired(String value) {
        if (value == null) {
            throw new IllegalArgumentException("search predicate values must not be null");
        }
        return value.trim();
    }

    @Builder
    public record SelectedSchedule(
            CourseDayOfWeek dayOfWeek,
            String startTime,
            String endTime
    ) {
    }
}
