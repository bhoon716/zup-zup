package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {
    List<Course> findByNameContainingOrProfessorContaining(String name, String professor);

    Optional<Course> findByCourseKey(String courseKey);

    boolean existsByCourseKey(String courseKey);

    List<Course> findByCourseKeyIn(List<String> courseKeys);
}
