package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String>, CourseRepositoryCustom {
    List<Course> findByNameContainingOrProfessorContaining(String name, String professor);
}
