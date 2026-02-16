package bhoon.sugang_helper.domain.course.repository;

import static bhoon.sugang_helper.domain.course.entity.QCourse.course;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.QCourseSchedule;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.request.ScheduleCondition;
import bhoon.sugang_helper.domain.wishlist.entity.QWishlist;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable) {
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

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression matchSelectedSchedules(List<ScheduleCondition> selectedSchedules) {
        List<ValidScheduleCondition> validConditions = toValidConditions(selectedSchedules);
        if (validConditions.isEmpty()) {
            return null;
        }

        QCourseSchedule schedule = QCourseSchedule.courseSchedule;
        BooleanBuilder selectedSlots = buildSelectedSlots(schedule, validConditions);

        return JPAExpressions.selectOne()
                .from(schedule)
                .where(schedule.course.eq(course)
                        .and(selectedSlots.not()))
                .notExists();
    }

    private List<ValidScheduleCondition> toValidConditions(List<ScheduleCondition> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return List.of();
        }

        return selectedSchedules.stream()
                .map(this::toValidCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ValidScheduleCondition> toValidCondition(ScheduleCondition condition) {
        if (condition.getDayOfWeek() == null) {
            return Optional.empty();
        }

        LocalTime startTime = parseTime(condition.getStartTime());
        LocalTime endTime = parseTime(condition.getEndTime());
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return Optional.empty();
        }

        return Optional.of(new ValidScheduleCondition(condition.getDayOfWeek(), startTime, endTime));
    }

    private BooleanBuilder buildSelectedSlots(QCourseSchedule schedule, List<ValidScheduleCondition> validConditions) {
        BooleanBuilder selectedSlots = new BooleanBuilder();
        for (ValidScheduleCondition validCondition : validConditions) {
            selectedSlots.or(schedule.dayOfWeek.eq(validCondition.dayOfWeek())
                    .and(schedule.startTime.goe(validCondition.startTime()))
                    .and(schedule.endTime.loe(validCondition.endTime())));
        }
        return selectedSlots;
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
        if (!StringUtils.hasText(classification)) {
            return null;
        }

        CourseClassification enumValue = CourseClassification.from(classification);
        return enumValue != null ? course.classification.eq(enumValue) : null;
    }

    private BooleanExpression containsDepartment(String department) {
        return StringUtils.hasText(department) ? course.department.contains(department) : null;
    }

    private BooleanExpression eqGradingMethod(String gradingMethod) {
        if (!StringUtils.hasText(gradingMethod)) {
            return null;
        }

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
        if (day == null) {
            return null;
        }

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

    private record ValidScheduleCondition(CourseDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
