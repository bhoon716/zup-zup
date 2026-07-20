package bhoon.sugang_helper.course.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends RepositoryContract<Course, Long>, CourseRepositoryCustom {

    Optional<Course> findByCourseKey(String courseKey);

    Optional<Course> findByCourseKeyForUpdate(String courseKey);

    boolean existsByCourseKey(String courseKey);

    List<Course> findByCourseKeyIn(List<String> courseKeys);

    List<Course> findBySubjectCodeAndProfessor(String subjectCode, String professor);

    Optional<LocalDateTime> findMaxLastCrawledAt();

    List<String> findDistinctGeneralCategories();

    List<String> findDistinctGeneralDetailsByCategory(String category);
}
