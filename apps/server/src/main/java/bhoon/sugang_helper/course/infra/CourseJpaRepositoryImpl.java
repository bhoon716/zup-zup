package bhoon.sugang_helper.course.infra;

import static bhoon.sugang_helper.course.domain.QCourse.course;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseClassification;
import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.CourseRepositoryCustom;
import bhoon.sugang_helper.course.domain.CourseStatus;
import bhoon.sugang_helper.course.domain.DisclosureStatus;
import bhoon.sugang_helper.course.domain.GradingMethod;
import bhoon.sugang_helper.course.domain.LectureLanguage;
import bhoon.sugang_helper.course.domain.QCourseSchedule;
import bhoon.sugang_helper.course.domain.TargetGrade;
import bhoon.sugang_helper.wishlist.domain.QWishlist;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class CourseJpaRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseJpaRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Slice<Course> searchCourses(CourseSearchCriteria condition, Pageable pageable) {
        String sortBy = condition.getSortBy();
        boolean isPopularSort = sortBy == null || "popular".equalsIgnoreCase(sortBy);
        QWishlist wishlist = QWishlist.wishlist;

        var query = queryFactory.selectFrom(course);
        if (isPopularSort) {
            query.leftJoin(wishlist).on(wishlist.courseKey.eq(course.courseKey));
        }

        query.where(
                containsName(condition.getName()),
                containsProfessor(condition.getProfessor()),
                eqSubjectCode(condition.getSubjectCode()),
                eqAcademicYear(condition.getAcademicYear()),
                eqSemester(condition.getSemester()),
                inClassifications(condition.getClassifications()),
                containsDepartment(condition.getDepartment()),
                inGradingMethods(condition.getGradingMethods()),
                inLectureLanguages(condition.getLectureLanguages()),
                isAvailable(condition.getIsAvailableOnly()),
                eqDayOfWeek(condition.getDayOfWeek()),
                inCredits(condition.getCredits()),
                goeMinCredits(condition.getMinCredits()),
                inTargetGrades(condition.getTargetGrades()),
                eqDisclosure(condition.getDisclosure()),
                eqLectureHours(condition.getLectureHours()),
                goeMinLectureHours(condition.getMinLectureHours()),
                eqGeneralCategory(condition.getGeneralCategory()),
                eqGeneralDetail(condition.getGeneralDetail()),
                inStatuses(condition.getStatuses()),
                containsCourseDirection(condition.getCourseDirection()),
                matchSelectedSchedules(condition.getSelectedSchedules()),
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

    private BooleanExpression matchSelectedSchedules(List<CourseSearchCriteria.SelectedSchedule> selectedSchedules) {
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

    private List<ValidScheduleCondition> toValidConditions(List<CourseSearchCriteria.SelectedSchedule> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return List.of();
        }

        return selectedSchedules.stream()
                .map(this::toValidCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ValidScheduleCondition> toValidCondition(CourseSearchCriteria.SelectedSchedule condition) {
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
        return containsAnyText(course.professor, professor);
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

    private <T extends Enum<T>> List<T> toEnumList(List<String> values, Function<String, T> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CourseClassification> toClassifications(List<String> values) {
        return toEnumList(values, CourseClassification::from);
    }

    private BooleanExpression inClassifications(List<String> classifications) {
        List<CourseClassification> values = toClassifications(classifications);
        return values.isEmpty() ? null : course.classification.in(values);
    }

    private BooleanExpression containsDepartment(String department) {
        return containsAnyText(course.department, department);
    }

    private List<GradingMethod> toGradingMethods(List<String> values) {
        return toEnumList(values, GradingMethod::from);
    }

    private BooleanExpression inGradingMethods(List<String> gradingMethods) {
        List<GradingMethod> values = toGradingMethods(gradingMethods);
        return values.isEmpty() ? null : course.gradingMethod.in(values);
    }

    private List<LectureLanguage> toLectureLanguages(List<String> values) {
        return toEnumList(values, LectureLanguage::from);
    }

    private BooleanExpression inLectureLanguages(List<String> lectureLanguages) {
        List<LectureLanguage> values = toLectureLanguages(lectureLanguages);
        return values.isEmpty() ? null : course.lectureLanguage.in(values);
    }

    private BooleanExpression isAvailable(Boolean isAvailableOnly) {
        if (isAvailableOnly == null || !isAvailableOnly) {
            return null;
        }
        return course.capacity.gt(course.current);
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

    private BooleanExpression inCredits(List<String> credits) {
        return credits != null && !credits.isEmpty() ? course.credits.in(credits) : null;
    }

    private BooleanExpression goeMinCredits(Double minCredits) {
        if (minCredits == null) {
            return null;
        }
        return course.credits.castToNum(Double.class).goe(minCredits);
    }

    private BooleanExpression inTargetGrades(List<TargetGrade> targetGrades) {
        if (targetGrades == null || targetGrades.isEmpty()) {
            return null;
        }
        return course.targetGrade.in(targetGrades);
    }

    private BooleanExpression eqDisclosure(String disclosure) {
        if (!StringUtils.hasText(disclosure)) {
            return null;
        }
        DisclosureStatus value = DisclosureStatus.from(disclosure);
        return value == null ? null : course.disclosure.eq(value);
    }

    private BooleanExpression eqLectureHours(Integer lectureHours) {
        if (lectureHours == null) {
            return null;
        }
        return course.lectureHours.eq(lectureHours);
    }

    private BooleanExpression goeMinLectureHours(Integer minLectureHours) {
        if (minLectureHours == null) {
            return null;
        }
        return course.lectureHours.goe(minLectureHours);
    }

    private BooleanExpression eqGeneralCategory(String generalCategory) {
        return StringUtils.hasText(generalCategory) ? course.generalCategory.eq(generalCategory) : null;
    }

    private BooleanExpression eqGeneralDetail(String generalDetail) {
        return StringUtils.hasText(generalDetail) ? course.generalDetail.eq(generalDetail) : null;
    }

    private List<CourseStatus> toStatuses(List<String> values) {
        return toEnumList(values, CourseStatus::from);
    }

    private BooleanExpression inStatuses(List<String> statuses) {
        List<CourseStatus> values = toStatuses(statuses);
        return values.isEmpty() ? null : course.status.in(values);
    }

    private BooleanExpression containsCourseDirection(String courseDirection) {
        return containsAnyText(course.courseDirection, courseDirection);
    }

    private BooleanExpression inWishlist(Boolean wishedOnly, Long userId) {
        if (wishedOnly == null || !wishedOnly || userId == null) {
            return null;
        }
        QWishlist wishlist = QWishlist.wishlist;
        return JPAExpressions.selectOne()
                .from(wishlist)
                .where(wishlist.userId.eq(userId)
                        .and(wishlist.courseKey.eq(course.courseKey)))
                .exists();
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(CourseSearchCriteria condition, QWishlist joinedWishlist) {
        String sortBy = condition.getSortBy();
        String sortOrder = condition.getSortOrder();
        Order order = "asc".equalsIgnoreCase(sortOrder) ? Order.ASC : Order.DESC;

        if (sortBy == null || "popular".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(order, popularCount(joinedWishlist)),
                    new OrderSpecifier<>(Order.DESC, course.current),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("name".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(order, course.name),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("rating".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(order, course.averageRating),
                    new OrderSpecifier<>(Order.DESC, course.reviewCount),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("current".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(order, course.current),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        if ("available".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(order, course.capacity.subtract(course.current)),
                    new OrderSpecifier<>(Order.ASC, course.name),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        return new OrderSpecifier<?>[]{
                new OrderSpecifier<>(order, popularCount(joinedWishlist)),
                new OrderSpecifier<>(Order.DESC, course.current),
                new OrderSpecifier<>(Order.ASC, course.courseKey)
        };
    }

    private NumberExpression<Long> popularCount(QWishlist joinedWishlist) {
        if (joinedWishlist != null) {
            return joinedWishlist.id.count();
        }
        return course.reviewCount.longValue();
    }

    private BooleanExpression containsAnyText(StringExpression field, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        List<String> terms = splitSearchTerms(value);
        if (terms.isEmpty()) {
            return null;
        }
        BooleanExpression expression = null;
        for (String term : terms) {
            BooleanExpression predicate = field.contains(term);
            expression = expression == null ? predicate : expression.or(predicate);
        }
        return expression;
    }

    private List<String> splitSearchTerms(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }

        return Arrays.stream(rawValue.split("[,\\n;]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
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

    private record ValidScheduleCondition(CourseDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
