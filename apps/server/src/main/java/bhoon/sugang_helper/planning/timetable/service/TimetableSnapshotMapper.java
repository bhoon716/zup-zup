package bhoon.sugang_helper.planning.timetable.service;

import bhoon.sugang_helper.academic.api.catalog.CourseScheduleSummary;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.planning.api.PrimaryTimetableSnapshot;
import bhoon.sugang_helper.planning.api.TimetableCourseScheduleSnapshot;
import bhoon.sugang_helper.planning.api.TimetableCourseSnapshot;
import bhoon.sugang_helper.planning.api.TimetableCustomScheduleSnapshot;
import bhoon.sugang_helper.planning.api.TimetableCustomScheduleTimeSnapshot;
import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.planning.timetable.entity.CustomScheduleTime;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import java.util.List;

final class TimetableSnapshotMapper {

    private TimetableSnapshotMapper() {
    }

    static PrimaryTimetableSnapshot from(Timetable timetable, List<CourseSummary> courses,
                                         List<CustomSchedule> customSchedules, String totalCredits) {
        return new PrimaryTimetableSnapshot(
                timetable.getId(), timetable.getName(), timetable.isPrimary(),
                courses.stream().map(TimetableSnapshotMapper::toCourseSnapshot).toList(),
                customSchedules.stream().map(TimetableSnapshotMapper::toCustomScheduleSnapshot).toList(),
                totalCredits);
    }

    private static TimetableCourseSnapshot toCourseSnapshot(CourseSummary course) {
        return new TimetableCourseSnapshot(
                course.courseKey(), course.name(), course.professor(), course.classTime(), course.credits(),
                course.classification(), course.classroom(),
                course.schedules().stream().map(TimetableSnapshotMapper::toCourseScheduleSnapshot).toList());
    }

    private static TimetableCourseScheduleSnapshot toCourseScheduleSnapshot(CourseScheduleSummary schedule) {
        return new TimetableCourseScheduleSnapshot(schedule.dayOfWeek(), schedule.startTime(), schedule.endTime());
    }

    private static TimetableCustomScheduleSnapshot toCustomScheduleSnapshot(CustomSchedule schedule) {
        return new TimetableCustomScheduleSnapshot(schedule.getId(), schedule.getTitle(), schedule.getProfessor(),
                schedule.getTimes().stream().map(TimetableSnapshotMapper::toCustomScheduleTimeSnapshot).toList());
    }

    private static TimetableCustomScheduleTimeSnapshot toCustomScheduleTimeSnapshot(CustomScheduleTime time) {
        return new TimetableCustomScheduleTimeSnapshot(
                time.getId(), time.getDayOfWeek(), time.getStartTime(), time.getEndTime(), time.getClassroom());
    }
}
