package bhoon.sugang_helper.academic.course.repository;

import static bhoon.sugang_helper.academic.course.entity.QCourse.course;

import bhoon.sugang_helper.academic.course.dto.criteria.CourseSelectedSchedule;
import bhoon.sugang_helper.academic.course.entity.QCourseSchedule;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
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
import org.springframework.util.StringUtils;

final class SelectedSchedulePredicateBuilder {

    BooleanExpression build(List<CourseSelectedSchedule> selectedSchedules) {
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
            mergedAll.addAll(mergeDayConditions(entry.getKey(), entry.getValue()));
        }
        return mergedAll;
    }

    private List<ValidScheduleCondition> mergeDayConditions(
            CourseDayOfWeek day, List<ValidScheduleCondition> conditions) {
        List<ValidScheduleCondition> sorted = new ArrayList<>(conditions);
        sorted.sort(Comparator.comparing(ValidScheduleCondition::startTime));

        List<ValidScheduleCondition> merged = new ArrayList<>();
        ValidScheduleCondition current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            ValidScheduleCondition next = sorted.get(i);
            if (next.startTime().isAfter(current.endTime())) {
                merged.add(current);
                current = next;
                continue;
            }

            LocalTime endTime = current.endTime();
            if (next.endTime().isAfter(endTime)) {
                endTime = next.endTime();
            }
            current = new ValidScheduleCondition(day, current.startTime(), endTime);
        }
        merged.add(current);
        return merged;
    }

    private List<ValidScheduleCondition> toValidConditions(
            List<CourseSelectedSchedule> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return List.of();
        }

        return selectedSchedules.stream()
                .map(this::toValidCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ValidScheduleCondition> toValidCondition(CourseSelectedSchedule condition) {
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
        if (!StringUtils.hasText(time)) {
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
