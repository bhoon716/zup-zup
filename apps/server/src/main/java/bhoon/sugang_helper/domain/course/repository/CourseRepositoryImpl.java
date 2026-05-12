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

        // 시간표 검색 조건이 있을 때는 반드시 시간 정보가 있는 강의만 조회하도록 수정
        return course.schedules.isNotEmpty()
                .and(JPAExpressions.selectOne()
                        .from(schedule)
                        .where(schedule.course.eq(course)
                                .and(selectedSlots.not()))
                        .notExists());
    }

    /**
     * 검색 조건을 QueryDSL에서 사용 가능한 유효한 시간대 조건으로 변환
     */
    private List<ValidScheduleCondition> toValidConditions(List<ScheduleCondition> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return List.of();
        }

        return selectedSchedules.stream()
                .map(this::toValidCondition)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 단일 시간대 필터 조건을 유효한 포맷으로 변환
     */
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

    /**
     * 선택된 슬롯들을 BooleanBuilder로 결합하여 쿼리 조건 생성
     */
    private BooleanBuilder buildSelectedSlots(QCourseSchedule schedule, List<ValidScheduleCondition> validConditions) {
        BooleanBuilder selectedSlots = new BooleanBuilder();
        for (ValidScheduleCondition validCondition : validConditions) {
            selectedSlots.or(schedule.dayOfWeek.eq(validCondition.dayOfWeek())
                    .and(schedule.startTime.goe(validCondition.startTime()))
                    .and(schedule.endTime.loe(validCondition.endTime())));
        }
        return selectedSlots;
    }

    /**
     * 과목명 포함 여부 필터
     */
    private BooleanExpression containsName(String name) {
        return StringUtils.hasText(name) ? course.name.contains(name) : null;
    }

    /**
     * 교수명 포함 여부 필터
     */
    private BooleanExpression containsProfessor(String professor) {
        return StringUtils.hasText(professor) ? course.professor.contains(professor) : null;
    }

    /**
     * 과목 코드 일치 여부 필터
     */
    private BooleanExpression eqSubjectCode(String subjectCode) {
        return StringUtils.hasText(subjectCode) ? course.subjectCode.eq(subjectCode) : null;
    }

    /**
     * 연도 일치 여부 필터
     */
    private BooleanExpression eqAcademicYear(String year) {
        return StringUtils.hasText(year) ? course.academicYear.eq(year) : null;
    }

    /**
     * 학기 일치 여부 필터
     */
    private BooleanExpression eqSemester(String semester) {
        return StringUtils.hasText(semester) ? course.semester.eq(semester) : null;
    }

    /**
     * 문자열 리스트를 특정 Enum 리스트로 변환 (null 및 유효하지 않은 값 제외)
     */
    private <T extends Enum<T>> List<T> toEnumList(List<String> values, java.util.function.Function<String, T> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(mapper)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 이수구분 리스트 포함 여부 필터
     */
    private BooleanExpression inClassifications(List<String> classifications) {
        List<CourseClassification> enumValues = toEnumList(classifications, CourseClassification::from);
        return !enumValues.isEmpty() ? course.classification.in(enumValues) : null;
    }

    /**
     * 학과명 포함 여부 필터
     */
    private BooleanExpression containsDepartment(String department) {
        return StringUtils.hasText(department) ? course.department.contains(department) : null;
    }

    /**
     * 성적평가방식 리스트 포함 여부 필터
     */
    private BooleanExpression inGradingMethods(List<String> gradingMethods) {
        List<GradingMethod> enumValues = toEnumList(gradingMethods, GradingMethod::from);
        return !enumValues.isEmpty() ? course.gradingMethod.in(enumValues) : null;
    }

    /**
     * 강의언어 리스트 포함 여부 필터
     */
    private BooleanExpression inLectureLanguages(List<String> lectureLanguages) {
        List<LectureLanguage> enumValues = toEnumList(lectureLanguages, LectureLanguage::from);
        return !enumValues.isEmpty() ? course.lectureLanguage.in(enumValues) : null;
    }

    /**
     * 강의 요일 필터 (해당 요일에 수업이 하나라도 있는 경우)
     */
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

    /**
     * 문자열 시간(HH:mm)을 LocalTime으로 파싱
     */
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

    /**
     * 여석 존재 여부 필터
     */
    private BooleanExpression isAvailable(Boolean isAvailableOnly) {
        if (isAvailableOnly == null || !isAvailableOnly) {
            return null;
        }
        return course.capacity.gt(course.current);
    }

    /**
     * 학점 리스트 포함 여부 필터
     */
    private BooleanExpression inCredits(List<String> credits) {
        return credits != null && !credits.isEmpty() ? course.credits.in(credits) : null;
    }

    /**
     * 강의 시수 일치 여부 필터
     */
    private BooleanExpression eqLectureHours(Integer lectureHours) {
        return lectureHours != null ? course.lectureHours.eq(lectureHours) : null;
    }

    /**
     * 최소 강의 시수 이상 필터
     */
    private BooleanExpression goeMinLectureHours(Integer minLectureHours) {
        return minLectureHours != null ? course.lectureHours.goe(minLectureHours) : null;
    }

    /**
     * 교양영역구분 일치 여부 필터
     */
    private BooleanExpression eqGeneralCategory(String generalCategory) {
        return StringUtils.hasText(generalCategory) ? course.generalCategory.eq(generalCategory) : null;
    }

    /**
     * 교양세부영역 일치 여부 필터
     */
    private BooleanExpression eqGeneralDetail(String generalDetail) {
        return StringUtils.hasText(generalDetail) ? course.generalDetail.eq(generalDetail) : null;
    }

    /**
     * 설강 상태 리스트 포함 여부 필터
     */
    private BooleanExpression inStatuses(List<String> statusStrs) {
        List<CourseStatus> statuses = toEnumList(statusStrs, CourseStatus::from);
        return !statuses.isEmpty() ? course.status.in(statuses) : null;
    }

    /**
     * 수업운영방향 포함 여부 필터
     */
    private BooleanExpression containsCourseDirection(String courseDirection) {
        return StringUtils.hasText(courseDirection) ? course.courseDirection.contains(courseDirection) : null;
    }

    /**
     * 관심 강의(찜) 필터
     */
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

    /**
     * 동적 정렬 조건 생성
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(CourseSearchCondition condition, QWishlist joinedWishlist) {
        String sortBy = condition.getSortBy();
        String sortOrder = condition.getSortOrder();

        Order order = "desc".equalsIgnoreCase(sortOrder) ? Order.DESC : Order.ASC;

        // 강의명 순: 강의명(order) -> 과목코드(ASC) -> 분반(ASC)
        if ("name".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(order, course.name),
                    new OrderSpecifier<>(Order.ASC, course.subjectCode),
                    new OrderSpecifier<>(Order.ASC, course.classNumber)
            };
        }

        // 인기순 (찜 횟수 기준): 찜횟수(order) -> 신청인원(DESC) -> 고유키(ASC)
        if ("popular".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(order,
                            joinedWishlist != null ? joinedWishlist.id.count()
                                    : JPAExpressions.select(QWishlist.wishlist.count())
                                            .from(QWishlist.wishlist)
                                            .where(QWishlist.wishlist.courseKey.eq(course.courseKey))),
                    new OrderSpecifier<>(Order.DESC, course.current),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        // 신청인원순: 신청인원(order) -> 전체정원(ASC) -> 고유키(ASC)
        if ("current".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(order, course.current),
                    new OrderSpecifier<>(Order.ASC, course.capacity),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        // 평점순: 평점(order) -> 리뷰수(DESC) -> 고유키(ASC)
        if ("rating".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(order, course.averageRating),
                    new OrderSpecifier<>(Order.DESC, course.reviewCount),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        // 여석순: 여석수(order) -> 전체정원(ASC) -> 고유키(ASC)
        if ("available".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[]{
                    new OrderSpecifier<>(order, course.capacity.subtract(course.current)),
                    new OrderSpecifier<>(Order.ASC, course.capacity),
                    new OrderSpecifier<>(Order.ASC, course.courseKey)
            };
        }

        // default: 인기순 기반 정렬
        return new OrderSpecifier[]{
                new OrderSpecifier<>(order,
                        joinedWishlist != null ? joinedWishlist.id.count()
                                : JPAExpressions.select(QWishlist.wishlist.count())
                                        .from(QWishlist.wishlist)
                                        .where(QWishlist.wishlist.courseKey.eq(course.courseKey))),
                new OrderSpecifier<>(Order.DESC, course.current),
                new OrderSpecifier<>(Order.ASC, course.courseKey)
        };
    }

    /**
     * 최소 학점 이상 필터 (학점 정보가 문자열이므로 변환 후 비교)
     */
    private BooleanExpression goeMinCredits(Double minCredits) {
        if (minCredits == null) {
            return null;
        }
        // JBNU 학점(credits)은 "1", "3", "0.5" 등 문자열 형태임. 숫자로 변환하여 비교 수행.
        return course.credits.castToNum(Double.class).goe(minCredits);
    }

    /**
     * 대상 학년 리스트 포함 여부 필터
     */
    private BooleanExpression inTargetGrades(List<TargetGrade> targetGrades) {
        if (targetGrades == null || targetGrades.isEmpty()) {
            return null;
        }
        return course.targetGrade.in(targetGrades);
    }

    /**
     * 공개 여부 및 비공개 사유 일치 여부 필터
     */
    private BooleanExpression eqDisclosure(String disclosureStr) {
        if (!StringUtils.hasText(disclosureStr)) {
            return null;
        }
        DisclosureStatus status = DisclosureStatus.from(disclosureStr);
        return status != null ? course.disclosure.eq(status) : null;
    }

    /**
     * 유효한 시간대 조건을 담는 내부 레코드
     */
    private record ValidScheduleCondition(CourseDayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
