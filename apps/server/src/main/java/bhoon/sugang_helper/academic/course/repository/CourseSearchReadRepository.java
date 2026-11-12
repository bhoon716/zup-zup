package bhoon.sugang_helper.academic.course.repository;

import static bhoon.sugang_helper.academic.course.dto.criteria.CourseSearchCriteria.POPULAR_SORT;
import static bhoon.sugang_helper.academic.course.entity.QCourse.course;

import bhoon.sugang_helper.academic.api.search.CourseWishlistQuery;
import bhoon.sugang_helper.academic.course.dto.criteria.CourseSearchCriteria;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class CourseSearchReadRepository {

    private final JPAQueryFactory queryFactory;
    private final CourseWishlistQuery wishlistQuery;
    private final SelectedSchedulePredicateBuilder selectedSchedulePredicateBuilder;

    public CourseSearchReadRepository(EntityManager em, CourseWishlistQuery wishlistQuery) {
        this.queryFactory = new JPAQueryFactory(em);
        this.wishlistQuery = wishlistQuery;
        this.selectedSchedulePredicateBuilder = new SelectedSchedulePredicateBuilder();
    }

    public Slice<Course> searchCourses(CourseSearchCriteria condition, Pageable pageable) {
        List<Course> content = queryFactory.selectFrom(course)
                .where(searchPredicates(condition))
                .orderBy(getOrderSpecifiers(condition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.remove(pageable.getPageSize());
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression[] searchPredicates(CourseSearchCriteria condition) {
        return new BooleanExpression[]{
                containsKeyword(condition.keyword()),
                containsName(condition.name()),
                containsAnyText(course.professor, condition.professor()),
                eqSubjectCode(condition.subjectCode()),
                eqAcademicYear(condition.academicYear()),
                eqSemester(condition.semester()),
                inClassifications(condition.classifications()),
                containsAnyText(course.department, condition.department()),
                inGradingMethods(condition.gradingMethods()),
                inLectureLanguages(condition.lectureLanguages()),
                isAvailable(condition.isAvailableOnly()),
                eqDayOfWeek(condition.dayOfWeek()),
                inCredits(condition.credits()),
                goeMinCredits(condition.minCredits()),
                inTargetGrades(condition.targetGrades()),
                eqDisclosure(condition.disclosure()),
                eqLectureHours(condition.lectureHours()),
                goeMinLectureHours(condition.minLectureHours()),
                eqGeneralCategory(condition.generalCategory()),
                eqGeneralDetail(condition.generalDetail()),
                inStatuses(condition.statuses()),
                containsAnyTerms(course.courseDirection, condition.courseDirectionTerms()),
                selectedSchedulePredicateBuilder.build(condition.selectedSchedules()),
                inWishlist(condition.isWishedOnly(), condition.userId())
        };
    }

    private BooleanExpression containsName(String name) {
        return StringUtils.hasText(name) ? course.name.contains(name) : null;
    }

    private BooleanExpression containsKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? course.name.contains(keyword.trim()) : null;
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

    private BooleanExpression inWishlist(Boolean wishedOnly, Long userId) {
        if (wishedOnly == null || !wishedOnly || userId == null) {
            return null;
        }
        return wishlistQuery.wishedBy(course.courseKey, userId);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(CourseSearchCriteria condition) {
        String sortBy = condition.sortBy();
        Order order = "asc".equalsIgnoreCase(condition.sortOrder()) ? Order.ASC : Order.DESC;

        if (POPULAR_SORT.equals(sortBy)) {
            return rankedOrder(order, wishlistQuery.popularity(course.courseKey));
        }

        if ("name".equalsIgnoreCase(sortBy)) {
            return orderWithCourseKey(new OrderSpecifier<>(order, course.name));
        }

        if ("rating".equalsIgnoreCase(sortBy)) {
            return orderWithCourseKey(
                    new OrderSpecifier<>(order, course.averageRating),
                    new OrderSpecifier<>(Order.DESC, course.reviewCount));
        }

        if ("current".equalsIgnoreCase(sortBy)) {
            return orderWithCourseKey(new OrderSpecifier<>(order, course.current));
        }

        if ("available".equalsIgnoreCase(sortBy)) {
            return orderWithCourseKey(
                    new OrderSpecifier<>(order, course.capacity.subtract(course.current)),
                    new OrderSpecifier<>(Order.ASC, course.name));
        }

        return rankedOrder(order, course.reviewCount.longValue());
    }

    private OrderSpecifier<?>[] rankedOrder(Order order, NumberExpression<Long> ranking) {
        return orderWithCourseKey(
                new OrderSpecifier<>(order, ranking),
                new OrderSpecifier<>(Order.DESC, course.current));
    }

    private OrderSpecifier<?>[] orderWithCourseKey(OrderSpecifier<?> primary) {
        return new OrderSpecifier<?>[]{primary, new OrderSpecifier<>(Order.ASC, course.courseKey)};
    }

    private OrderSpecifier<?>[] orderWithCourseKey(OrderSpecifier<?> primary, OrderSpecifier<?> secondary) {
        return new OrderSpecifier<?>[]{primary, secondary, new OrderSpecifier<>(Order.ASC, course.courseKey)};
    }

    private BooleanExpression containsAnyText(StringExpression field, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        List<String> terms = splitSearchTerms(value);
        return containsAnyTerms(field, terms);
    }

    private BooleanExpression containsAnyTerms(StringExpression field, List<String> terms) {
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

}
