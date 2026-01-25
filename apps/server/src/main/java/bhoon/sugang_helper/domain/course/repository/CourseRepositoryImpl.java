package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.enums.LectureType;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

import static bhoon.sugang_helper.domain.course.entity.QCourse.course;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Course> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        List<Course> content = queryFactory
                .selectFrom(course)
                .where(
                        containsKeyword(condition.getKeyword()),
                        eqAcademicYear(condition.getAcademicYear()),
                        eqSemester(condition.getSemester()),
                        eqClassification(condition.getClassification()),
                        containsDepartment(condition.getDepartment()),
                        eqGradingMethod(condition.getGradingMethod()),
                        eqLectureType(condition.getLectureType()),
                        eqLectureLanguage(condition.getLectureLanguage()),
                        isAvailable(condition.getIsAvailableOnly()),
                        eqDayOfWeek(condition.getDayOfWeek()),
                        eqPeriod(condition.getPeriod()))
                .orderBy(course.courseKey.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(course)
                .where(
                        containsKeyword(condition.getKeyword()), // Use global keyword search here too or specific
                                                                 // fields? The previous code mixed them. Let's use
                                                                 // unified approach or specific if condition has them.
                        // Assuming frontend sends EITHER keyword OR specific fields.
                        containsName(condition.getName()),
                        containsProfessor(condition.getProfessor()),
                        eqSubjectCode(condition.getSubjectCode()),
                        eqAcademicYear(condition.getAcademicYear()),
                        eqSemester(condition.getSemester()),
                        eqClassification(condition.getClassification()),
                        containsDepartment(condition.getDepartment()),
                        eqGradingMethod(condition.getGradingMethod()),
                        eqLectureType(condition.getLectureType()),
                        eqLectureLanguage(condition.getLectureLanguage()),
                        isAvailable(condition.getIsAvailableOnly()),
                        eqDayOfWeek(condition.getDayOfWeek()),
                        eqPeriod(condition.getPeriod()))
                .fetch().size(); // 대량 데이터 시 count 쿼리 최적화 필요하겠으나 MVP 수준에선 fetch().size()로 시작

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return course.name.contains(keyword)
                .or(course.professor.contains(keyword))
                .or(course.subjectCode.contains(keyword));
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

    private BooleanExpression eqLectureType(String lectureType) {
        if (!StringUtils.hasText(lectureType))
            return null;
        LectureType enumValue = LectureType.from(lectureType);
        return enumValue != null ? course.lectureType.eq(enumValue) : null;
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
