package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
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

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static bhoon.sugang_helper.domain.course.entity.QCourse.course;
import bhoon.sugang_helper.domain.course.entity.QCourseSchedule;
import bhoon.sugang_helper.domain.wishlist.entity.QWishlist;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public org.springframework.data.domain.Slice<Course> searchCourses(CourseSearchCondition condition,
            org.springframework.data.domain.Pageable pageable) {
        List<Course> content = queryFactory
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
                        eqCredits(condition.getCredits()),
                        eqLectureHours(condition.getLectureHours()),
                        goeMinLectureHours(condition.getMinLectureHours()),
                        eqGeneralCategory(condition.getGeneralCategory()),
                        eqGeneralDetail(condition.getGeneralDetail()),
                        matchSelectedSchedules(condition.getSelectedSchedules()),
                        inWishlist(condition.getIsWishedOnly(), condition.getUserId()))
                .orderBy(course.courseKey.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new org.springframework.data.domain.SliceImpl<>(content, pageable, hasNext);
    }

    // ... skipping other methods for brevity, keeping only the logic for subset
    // matching

    private BooleanExpression matchSelectedSchedules(List<ScheduleCondition> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return null;
        }

        // 과목의 모든 수업시간(dayOfWeek, start~end)이 선택된 시간대에 포함되어야 한다.
        QCourseSchedule schedule = QCourseSchedule.courseSchedule;

        com.querydsl.core.BooleanBuilder selectedSlots = new com.querydsl.core.BooleanBuilder();
        int validSlotCount = 0;
        for (ScheduleCondition cond : selectedSchedules) {
            if (cond.getDayOfWeek() == null) {
                continue;
            }

            LocalTime startTime = parseTime(cond.getStartTime());
            LocalTime endTime = parseTime(cond.getEndTime());
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                continue;
            }

            selectedSlots.or(schedule.dayOfWeek.eq(cond.getDayOfWeek())
                    .and(schedule.startTime.goe(startTime))
                    .and(schedule.endTime.loe(endTime)));
            validSlotCount++;
        }

        if (validSlotCount == 0) {
            return null;
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

    private LocalTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException first) {
            try {
                return LocalTime.parse(value + ":00");
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private BooleanExpression isAvailable(Boolean isAvailableOnly) {
        if (isAvailableOnly == null || !isAvailableOnly) {
            return null;
        }
        return course.capacity.gt(course.current);
    }

    private BooleanExpression eqCredits(String credits) {
        return StringUtils.hasText(credits) ? course.credits.eq(credits) : null;
    }

    private BooleanExpression eqLectureHours(Integer lectureHours) {
        return lectureHours != null ? course.lectureHours.eq(lectureHours) : null;
    }

    private BooleanExpression goeMinLectureHours(Integer minLectureHours) {
        return minLectureHours != null ? course.lectureHours.goe(minLectureHours) : null;
    }

    private BooleanExpression eqGeneralCategory(String generalCategory) {
        return StringUtils.hasText(generalCategory) ? course.generalCategory.eq(generalCategory) : null;
    }

    private BooleanExpression eqGeneralDetail(String generalDetail) {
        return StringUtils.hasText(generalDetail) ? course.generalDetail.eq(generalDetail) : null;
    }

    private BooleanExpression inWishlist(Boolean isWishedOnly, Long userId) {
        if (isWishedOnly == null || !isWishedOnly || userId == null) {
            return null;
        }

        QWishlist wishlist = QWishlist.wishlist;
        return JPAExpressions.selectOne()
                .from(wishlist)
                .where(wishlist.courseKey.eq(course.courseKey)
                        .and(wishlist.userId.eq(userId)))
                .exists();
    }

}
