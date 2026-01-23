package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepositoryCustom {
    Page<Course> searchCourses(CourseSearchCondition condition, Pageable pageable);
}
