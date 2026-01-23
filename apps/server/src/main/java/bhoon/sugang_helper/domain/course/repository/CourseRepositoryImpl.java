package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.QCourse;
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
                        nameContains(condition.getName()),
                        professorContains(condition.getProfessor()),
                        subjectCodeEq(condition.getSubjectCode()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(course)
                .where(
                        nameContains(condition.getName()),
                        professorContains(condition.getProfessor()),
                        subjectCodeEq(condition.getSubjectCode()))
                .fetch().size(); // 대량 데이터 시 count 쿼리 최적화 필요하겠으나 MVP 수준에선 fetch().size()로 시작

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? course.name.contains(name) : null;
    }

    private BooleanExpression professorContains(String professor) {
        return StringUtils.hasText(professor) ? course.professor.contains(professor) : null;
    }

    private BooleanExpression subjectCodeEq(String subjectCode) {
        return StringUtils.hasText(subjectCode) ? course.subjectCode.eq(subjectCode) : null;
    }
}
