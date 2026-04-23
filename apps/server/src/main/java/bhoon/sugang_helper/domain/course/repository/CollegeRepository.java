package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByName(String name);
}
