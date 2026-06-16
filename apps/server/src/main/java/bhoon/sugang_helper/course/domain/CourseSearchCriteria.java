package bhoon.sugang_helper.course.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
public class CourseSearchCriteria {
    private final String name;
    private final String professor;
    private final String subjectCode;
    private final String academicYear;
    private final String semester;
    private final List<String> classifications;
    private final String department;
    private final List<String> gradingMethods;
    private final List<String> lectureLanguages;
    private final Boolean isAvailableOnly;
    private final String dayOfWeek;
    private final List<SelectedSchedule> selectedSchedules;
    private final List<String> credits;
    private final Integer lectureHours;
    private final Integer minLectureHours;
    private final String generalCategory;
    private final String generalDetail;
    private final Long timetableId;
    private final Boolean isWishedOnly;
    private final List<String> statuses;
    private final String courseDirection;
    private final Double minCredits;
    private final List<TargetGrade> targetGrades;
    private final String disclosure;
    private final String sortBy;
    private final String sortOrder;
    private final Long userId;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SelectedSchedule {
        private final CourseDayOfWeek dayOfWeek;
        private final String startTime;
        private final String endTime;
    }
}
