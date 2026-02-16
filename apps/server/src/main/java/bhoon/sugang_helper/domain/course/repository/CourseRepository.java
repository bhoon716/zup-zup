package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {

    Optional<Course> findByCourseKey(String courseKey);

    boolean existsByCourseKey(String courseKey);

    List<Course> findByCourseKeyIn(List<String> courseKeys);
}
