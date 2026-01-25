package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import java.util.List;

public interface CourseRepositoryCustom {
    List<Course> searchCourses(CourseSearchCondition condition);

}
