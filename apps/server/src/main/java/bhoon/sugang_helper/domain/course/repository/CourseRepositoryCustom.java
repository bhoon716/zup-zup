package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface CourseRepositoryCustom {
    Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable);
}
