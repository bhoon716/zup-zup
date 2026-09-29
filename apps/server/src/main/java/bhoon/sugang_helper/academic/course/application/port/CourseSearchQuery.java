package bhoon.sugang_helper.academic.course.application.port;

import bhoon.sugang_helper.academic.course.domain.Course;
import bhoon.sugang_helper.academic.course.domain.CourseSearchCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseSearchQuery {

    Slice<Course> searchCourses(CourseSearchCriteria criteria, Pageable pageable);
}
