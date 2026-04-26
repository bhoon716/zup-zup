package bhoon.sugang_helper.domain.course.dto;

import bhoon.sugang_helper.domain.course.enums.*;
import java.util.List;

/**
 * 파싱된 개별 강의 정보를 담는 DTO입니다.
 */
public record ParsedCourseDto(
    String courseKey,
    String subjectCode,
    String name,
    String classNumber,
    String professor,
    Integer capacity,
    Integer current,
    TargetGrade targetGrade,
    String academicYear,
    String semester,
    CourseClassification classification,
    String department,
    GradingMethod gradingMethod,
    String classTime,
    String credits,
    LectureLanguage lectureLanguage,
    DisclosureStatus disclosure,
    String disclosureReason,
    Integer lectureHours,
    String generalCategory,
    String generalDetail,
    CourseAccreditation accreditation,
    CourseStatus status,
    String classroom,
    Boolean hasSyllabus,
    String generalCategoryByYear,
    String courseDirection,
    String classDuration,
    List<ScheduleDto> schedules
) {
    /**
     * 강의 시간표 정보를 담는 DTO입니다.
     */
    public record ScheduleDto(
        CourseDayOfWeek dayOfWeek,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime
    ) {}
}
