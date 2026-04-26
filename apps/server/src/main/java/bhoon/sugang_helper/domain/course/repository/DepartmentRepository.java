package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.College;
import bhoon.sugang_helper.domain.course.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByNameAndCollege(String name, College college);
    List<Department> findByCollegeId(Long collegeId);
}
