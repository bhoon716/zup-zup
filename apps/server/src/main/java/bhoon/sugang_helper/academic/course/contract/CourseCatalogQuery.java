package bhoon.sugang_helper.academic.course.contract;

import java.util.List;
import java.util.Optional;

public interface CourseCatalogQuery {

    Optional<CourseSummary> findByCourseKey(String courseKey);

    List<CourseSummary> findByCourseKeyIn(List<String> courseKeys);

    List<CourseName> findNamesByCourseKeyIn(List<String> courseKeys);

    boolean existsByCourseKey(String courseKey);
}
