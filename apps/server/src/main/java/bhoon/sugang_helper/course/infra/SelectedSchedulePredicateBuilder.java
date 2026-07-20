package bhoon.sugang_helper.course.infra;

import static bhoon.sugang_helper.course.domain.QCourse.course;

import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.QCourseSchedule;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class SelectedSchedulePredicateBuilder {

    BooleanExpression build(List<CourseSearchCriteria.SelectedSchedule> selectedSchedules) {
        List<ValidScheduleCondition> validConditions = toValidConditions(selectedSchedules);
        if (validConditions.isEmpty()) {
            return null;
        }

        QCourseSchedule schedule = QCourseSchedule.courseSchedule;
        BooleanBuilder selectedSlots = buildSelectedSlots(schedule, mergeContiguousConditions(validConditions));

        return course.schedules.isNotEmpty()
                .and(JPAExpressions.selectOne()
                        .from(schedule)
                        .where(schedule.course.eq(course)
                                .and(selectedSlots.not()))
                        .notExists());
    }

    private List<ValidScheduleCondition> mergeContiguousConditions(List<ValidScheduleCondition> conditions) {
        if (conditions.size() <= 1) {
            return conditions;
        }

        Map<CourseDayOfWeek, List<ValidScheduleCondition>> grouped = conditions.stream()
                .collect(Collectors.groupingBy(ValidScheduleCondition::dayOfWeek));
        List<ValidScheduleCondition> mergedAll = new ArrayList<>();

        for (Map.Entry<CourseDayOfWeek, List<ValidScheduleCondition>> entry : grouped.entrySet()) {
            List<ValidScheduleCondition> dayConditions = new ArrayList<>(entry.getValue());
            dayConditions.sort(Comparator.comparing(ValidScheduleCondition::startTime));

            List<ValidScheduleCondition> mergedDay = new ArrayList<>();
            ValidScheduleCondition currentMerged = dayConditions.get(0);
            for (int i = 1; i < dayConditions.size(); i++) {
                ValidScheduleCondition next = dayConditions.get(i);
                if (!next.startTime().isAfter(currentMerged.endTime())) {
                    LocalTime newEndTime = next.endTime().isAfter(currentMerged.endTime())
                            ? next.endTime() : currentMerged.endTime();
                    currentMerged = new ValidScheduleCondition(
                            entry.getKey(), currentMerged.startTime(), newEndTime);
                } else {
                    mergedDay.add(currentMerged);
                    currentMerged = next;
                }
            }
            mergedDay.add(currentMerged);
            mergedAll.addAll(mergedDay);
        }

        return mergedAll;
    }

    private List<ValidScheduleCondition> toValidConditions(
            List<CourseSearchCriteria.SelectedSchedule> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return List.of();
        }

        return selectedSchedules.stream()
                .map(this::toValidCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ValidScheduleCondition> toValidCondition(CourseSearchCriteria.SelectedSchedule condition) {
        if (condition.dayOfWeek() == null) {
            return Optional.empty();
        }

        LocalTime startTime = parseTime(condition.startTime());
        LocalTime endTime = parseTime(condition.endTime());
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return Optional.empty();
        }

        return Optional.of(new ValidScheduleCondition(condition.dayOfWeek(), startTime, endTime));
    }

    private BooleanBuilder buildSelectedSlots(
            QCourseSchedule schedule, List<ValidScheduleCondition> validConditions) {
        BooleanBuilder selectedSlots = new BooleanBuilder();
        for (ValidScheduleCondition condition : validConditions) {
            selectedSlots.or(schedule.dayOfWeek.eq(condition.dayOfWeek())
                    .and(schedule.startTime.goe(condition.startTime()))
                    .and(schedule.endTime.loe(condition.endTime())));
        }
        return selectedSlots;
    }

    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record ValidScheduleCondition(
            CourseDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
