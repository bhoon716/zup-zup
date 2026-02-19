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
                        goeMinCredits(condition.getMinCredits()),
                        eqTargetGrade(condition.getTargetGrade()),
                        eqDisclosure(condition.getDisclosure()),
                        eqLectureHours(condition.getLectureHours()),
                        goeMinLectureHours(condition.getMinLectureHours()),
                        eqGeneralCategory(condition.getGeneralCategory()),
                        eqGeneralDetail(condition.getGeneralDetail()),
                        eqStatus(condition.getStatus()),
                        matchSelectedSchedules(condition.getSelectedSchedules()),
                        inWishlist(condition.getIsWishedOnly(), condition.getUserId()))
                .orderBy(getOrderSpecifiers(condition))
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
     * 이수구분 일치 여부 필터
     */
    private BooleanExpression eqClassification(String classification) {
        if (!StringUtils.hasText(classification)) {
            return null;
        }

        CourseClassification enumValue = CourseClassification.from(classification);
        return enumValue != null ? course.classification.eq(enumValue) : null;
    }

    /**
     * 학과명 포함 여부 필터
     */
    private BooleanExpression containsDepartment(String department) {
        return StringUtils.hasText(department) ? course.department.contains(department) : null;
    }

    /**
     * 성적평가방식 일치 여부 필터
     */
    private BooleanExpression eqGradingMethod(String gradingMethod) {
        if (!StringUtils.hasText(gradingMethod)) {
            return null;
        }

        GradingMethod enumValue = GradingMethod.from(gradingMethod);
        return enumValue != null ? course.gradingMethod.eq(enumValue) : null;
    }

    /**
     * 강의언어 일치 여부 필터
     */
    private BooleanExpression eqLectureLanguage(String lectureLanguage) {
        if (!StringUtils.hasText(lectureLanguage)) {
            return null;
        }
        return course.lectureLanguage.eq(LectureLanguage.from(lectureLanguage));
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
     * 학점 일치 여부 필터
     */
    private BooleanExpression eqCredits(String credits) {
        return StringUtils.hasText(credits) ? course.credits.eq(credits) : null;
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
     * 설강 상태 일치 여부 필터
     */
    private BooleanExpression eqStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }
        CourseStatus status = CourseStatus.from(statusStr);
        return status != null ? course.status.eq(status) : null;
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
    private OrderSpecifier<?>[] getOrderSpecifiers(CourseSearchCondition condition) {
        String sortBy = condition.getSortBy();
        String sortOrder = condition.getSortOrder();

        Order order = "desc".equalsIgnoreCase(sortOrder) ? Order.DESC : Order.ASC;

        if ("name".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] { new OrderSpecifier<>(order, course.name) };
        }
        if ("credits".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] { new OrderSpecifier<>(order, course.credits) };
        }
        if ("available".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier[] { new OrderSpecifier<>(order, course.capacity.subtract(course.current)) };
        }

        // default: 추천순 (courseKey asc)
        return new OrderSpecifier[] { new OrderSpecifier<>(Order.ASC, course.courseKey) };
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
     * 대상 학년 일치 여부 필터
     */
    private BooleanExpression eqTargetGrade(TargetGrade targetGrade) {
        if (targetGrade == null) {
            return null;
        }
        return course.targetGrade.eq(targetGrade);
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
