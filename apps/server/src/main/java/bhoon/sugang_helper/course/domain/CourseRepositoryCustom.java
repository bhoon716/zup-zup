package bhoon.sugang_helper.course.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseRepositoryCustom {

    Slice<Course> searchCourses(CourseSearchCriteria condition, Pageable pageable);
}
