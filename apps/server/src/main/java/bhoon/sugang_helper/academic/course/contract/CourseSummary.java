package bhoon.sugang_helper.academic.course.contract;

import java.util.List;

public record CourseSummary(
        String courseKey,
        String subjectCode,
        String classNumber,
        String name,
        String professor,
        Integer capacity,
        Integer current,
        String academicYear,
        String semester,
        String classification,
        String credits,
        String classTime,
        String classroom,
        List<CourseScheduleSummary> schedules) {

    public CourseSummary {
        schedules = schedules == null ? List.of() : List.copyOf(schedules);
    }

    public Integer available() {
        return Math.max(0, capacity - current);
    }

    public boolean isMatchingTarget(String year, String semesterCode) {
        return academicYear.equals(year) && semester.equals(semesterCode);
    }

    public record CourseScheduleSummary(String dayOfWeek, String startTime, String endTime) {
    }
}
