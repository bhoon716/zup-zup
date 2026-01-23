package bhoon.sugang_helper.domain.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import bhoon.sugang_helper.domain.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, String> {
}
