package bhoon.sugang_helper.course.domain;

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
        String courseDirection,
        Double minCredits,
        List<TargetGrade> targetGrades,
        String disclosure,
        String sortBy,
        String sortOrder,
        Long userId
) {
    @Builder
    public record SelectedSchedule(
            CourseDayOfWeek dayOfWeek,
            String startTime,
            String endTime
    ) {
    }
}
