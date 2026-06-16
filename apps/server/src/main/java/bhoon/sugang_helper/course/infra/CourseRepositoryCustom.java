package bhoon.sugang_helper.course.infra;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.presentation.CourseSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseRepositoryCustom {
    Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable);
}
