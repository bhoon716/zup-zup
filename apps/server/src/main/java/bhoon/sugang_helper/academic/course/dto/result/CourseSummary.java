package bhoon.sugang_helper.academic.course.dto.result;

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

    // PMD 7.5는 compact record 생성자의 암묵적 필드 대입을 인식하지 못한다.
    @SuppressWarnings("PMD.UnusedAssignment")
    public CourseSummary {
        schedules = schedules == null ? List.of() : List.copyOf(schedules);
    }

    public Integer available() {
        return Math.max(0, capacity - current);
    }

    public boolean isMatchingTarget(String year, String semesterCode) {
        return academicYear.equals(year) && semester.equals(semesterCode);
    }
}
