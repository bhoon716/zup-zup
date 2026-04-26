package bhoon.sugang_helper.domain.course.repository;

import static bhoon.sugang_helper.domain.course.entity.QCourse.course;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.QCourseSchedule;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.request.ScheduleCondition;
import bhoon.sugang_helper.domain.wishlist.entity.QWishlist;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
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

    /**
     * 강의 검색 로직 (Slice 페이징 적용)
     */
    @Override
    public Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        String sortBy = condition.getSortBy();
        boolean isPopularSort = sortBy == null || "popular".equalsIgnoreCase(sortBy);
        QWishlist wishlist = QWishlist.wishlist;

        var query = queryFactory
                .selectFrom(course);

        if (isPopularSort) {
            query.leftJoin(wishlist).on(wishlist.courseKey.eq(course.courseKey));
        }

        query.where(
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
                goeMinCredits(condition.getMinCredits()),
                eqTargetGrade(condition.getTargetGrade()),
                eqDisclosure(condition.getDisclosure()),
                eqLectureHours(condition.getLectureHours()),
                goeMinLectureHours(condition.getMinLectureHours()),
                eqGeneralCategory(condition.getGeneralCategory()),
                eqGeneralDetail(condition.getGeneralDetail()),
                eqStatus(condition.getStatus()),
                containsCourseDirection(condition.getCourseDirection()),
                matchSelectedSchedules(condition.getSelectedSchedules()),
                eqCollegeId(condition.getCollegeId()),
                eqDepartmentId(condition.getDepartmentId()),
                inWishlist(condition.getIsWishedOnly(), condition.getUserId()));

        if (isPopularSort) {
            query.groupBy(course.id);
        }

        query.orderBy(getOrderSpecifiers(condition, isPopularSort ? wishlist : null))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);

        List<Course> content = query.fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 선택된 시간표 슬롯 리스트를 기반으로 겹치지 않는 강의 필터링
     */
    private BooleanExpression matchSelectedSchedules(List<ScheduleCondition> selectedSchedules) {
        List<ValidScheduleCondition> validConditions = toValidConditions(selectedSchedules);
        if (validConditions.isEmpty()) {
            return null;
        }

        QCourseSchedule schedule = QCourseSchedule.courseSchedule;
        BooleanBuilder selectedSlots = buildSelectedSlots(schedule, validConditions);

        return course.schedules.isNotEmpty()
                .and(JPAExpressions.selectOne()
                        .from(schedule)
                        .where(schedule.course.eq(course)
                                .and(selectedSlots.not()))
                        .notExists());
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

    private BooleanExpression eqStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }
        CourseStatus status = CourseStatus.from(statusStr);
        return status != null ? course.status.eq(status) : null;
    }

    private BooleanExpression containsCourseDirection(String courseDirection) {
        return StringUtils.hasText(courseDirection) ? course.courseDirection.contains(courseDirection) : null;
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

    private OrderSpecifier<?>[] getOrderSpecifiers(CourseSearchCondition condition, QWishlist joinedWishlist) {
        String sortBy = condition.getSortBy();
        String sortOrder = condition.getSortOrder();

        Order order = "desc".equalsIgnoreCase(sortOrder) ? Order.DESC : Order.ASC;

        if ("name".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] {
                    new OrderSpecifier<>(order, course.name),
                    new OrderSpecifier<>(Order.ASC, course.subjectCode),
                    new OrderSpecifier<>(Order.ASC, course.classNumber)
            };
        }

        if ("popular".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] {
                    new OrderSpecifier<>(order,
                            joinedWishlist != null ? joinedWishlist.id.count()
                                    : JPAExpressions.select(QWishlist.wishlist.count())
                                            .from(QWishlist.wishlist)
                                            .where(QWishlist.wishlist.courseKey.eq(course.courseKey))),
                    new OrderSpecifier<>(Order.DESC, course.current),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("current".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] {
                    new OrderSpecifier<>(order, course.current),
                    new OrderSpecifier<>(Order.ASC, course.capacity),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("available".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] {
                    new OrderSpecifier<>(order, course.capacity.subtract(course.current)),
                    new OrderSpecifier<>(Order.ASC, course.capacity),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        return new OrderSpecifier[] {
                new OrderSpecifier<>(order,
                        joinedWishlist != null ? joinedWishlist.id.count()
                                : JPAExpressions.select(QWishlist.wishlist.count())
                                        .from(QWishlist.wishlist)
                                        .where(QWishlist.wishlist.courseKey.eq(course.courseKey))),
                new OrderSpecifier<>(Order.DESC, course.current),
                new OrderSpecifier<>(Order.ASC, course.courseKey)
        };
    }

    private BooleanExpression goeMinCredits(Double minCredits) {
        if (minCredits == null) {
            return null;
        }
        return course.credits.castToNum(Double.class).goe(minCredits);
    }

    private BooleanExpression eqTargetGrade(TargetGrade targetGrade) {
        if (targetGrade == null) {
            return null;
        }
        return course.targetGrade.eq(targetGrade);
    }

    private BooleanExpression eqDisclosure(String disclosureStr) {
        if (!StringUtils.hasText(disclosureStr)) {
            return null;
        }
        DisclosureStatus status = DisclosureStatus.from(disclosureStr);
        return status != null ? course.disclosure.eq(status) : null;
    }

    private BooleanExpression eqCollegeId(Long collegeId) {
        return collegeId != null ? course.collegeId.eq(collegeId) : null;
    }

    private BooleanExpression eqDepartmentId(Long departmentId) {
        return departmentId != null ? course.departmentId.eq(departmentId) : null;
    }

    private record ValidScheduleCondition(CourseDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
