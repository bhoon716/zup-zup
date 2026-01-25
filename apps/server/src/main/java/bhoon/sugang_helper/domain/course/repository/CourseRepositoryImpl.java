package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.request.ScheduleCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.util.StringUtils;

import java.util.List;

import static bhoon.sugang_helper.domain.course.entity.QCourse.course;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Course> searchCourses(CourseSearchCondition condition) {
        return queryFactory
                .selectFrom(course)
                .where(
                        containsName(condition.getName()),
                        containsProfessor(condition.getProfessor()),
                        eqSubjectCode(condition.getSubjectCode()),
                        eqAcademicYear(condition.getAcademicYear()),
                        eqSemester(condition.getSemester()),
                        eqClassification(condition.getClassification()),
                        containsDepartment(condition.getDepartment()),
                        eqGradingMethod(condition.getGradingMethod()),
                        eqLectureLanguage(condition.getLectureLanguage()),
                        isAvailable(condition.getIsAvailableOnly()),
                        eqDayOfWeek(condition.getDayOfWeek()),
                        eqPeriod(condition.getPeriod()),
                        matchSelectedSchedules(condition.getSelectedSchedules()))
                .orderBy(course.courseKey.asc())
                .fetch();
    }

    // ... skipping other methods for brevity, keeping only the logic for subset
    // matching

    private BooleanExpression matchSelectedSchedules(List<ScheduleCondition> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return null;
        }

        // 과목의 모든 수업시간(dayOfWeek, period)이 선택된 리스트 내에 있어야 함 (Subset Matching)
        // 로직: "해당 과목의 수업 시간 중, 선택된 시간대에 포함되지 않는 시간이 존재하지 않아야 함"
        bhoon.sugang_helper.domain.course.entity.QCourseSchedule schedule = bhoon.sugang_helper.domain.course.entity.QCourseSchedule.courseSchedule;

        com.querydsl.core.BooleanBuilder selectedSlots = new com.querydsl.core.BooleanBuilder();
        for (ScheduleCondition cond : selectedSchedules) {
            selectedSlots.or(schedule.dayOfWeek.eq(cond.getDayOfWeek())
                    .and(schedule.period.eq(cond.getPeriod())));
        }

        return JPAExpressions.selectOne()
                .from(schedule)
                .where(schedule.course.eq(course)
                        .and(selectedSlots.not()))
                .notExists();
    }

    private BooleanExpression containsName(String name) {
        return StringUtils.hasText(name) ? course.name.contains(name) : null;
    }

    private BooleanExpression containsProfessor(String professor) {
        return StringUtils.hasText(professor) ? course.professor.contains(professor) : null;
    }

    private BooleanExpression eqSubjectCode(String subjectCode) {
        return StringUtils.hasText(subjectCode) ? course.subjectCode.eq(subjectCode) : null;
    }

    private BooleanExpression eqAcademicYear(String year) {
        return StringUtils.hasText(year) ? course.academicYear.eq(year) : null;
    }

    private BooleanExpression eqSemester(String semester) {
        return StringUtils.hasText(semester) ? course.semester.eq(semester) : null;
    }

    private BooleanExpression eqClassification(String classification) {
        if (!StringUtils.hasText(classification))
            return null;
        CourseClassification enumValue = CourseClassification.from(classification);
        return enumValue != null ? course.classification.eq(enumValue) : null;
    }

    private BooleanExpression containsDepartment(String department) {
        return StringUtils.hasText(department) ? course.department.contains(department) : null;
    }

    private BooleanExpression eqGradingMethod(String gradingMethod) {
        if (!StringUtils.hasText(gradingMethod))
            return null;
        GradingMethod enumValue = GradingMethod.from(gradingMethod);
        return enumValue != null ? course.gradingMethod.eq(enumValue) : null;
    }

    private BooleanExpression eqLectureLanguage(String lectureLanguage) {
        if (!StringUtils.hasText(lectureLanguage)) {
            return null;
        }
        return course.lectureLanguage.eq(LectureLanguage.from(lectureLanguage));
    }

    private BooleanExpression eqDayOfWeek(String dayOfWeekStr) {
        if (!StringUtils.hasText(dayOfWeekStr)) {
            return null;
        }
        CourseDayOfWeek day = CourseDayOfWeek.from(dayOfWeekStr);
        if (day == null)
            return null;
        return course.schedules.any().dayOfWeek.eq(day);
    }

    private BooleanExpression eqPeriod(String periodStr) {
        if (!StringUtils.hasText(periodStr)) {
            return null;
        }
        ClassPeriod period = ClassPeriod.from(periodStr);
        if (period == null)
            return null;
        return course.schedules.any().period.eq(period);
    }

    private BooleanExpression isAvailable(Boolean isAvailableOnly) {
        if (isAvailableOnly == null || !isAvailableOnly) {
            return null;
        }
        return course.capacity.gt(course.current);
    }
}
