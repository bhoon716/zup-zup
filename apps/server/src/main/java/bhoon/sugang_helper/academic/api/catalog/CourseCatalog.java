package bhoon.sugang_helper.academic.api.catalog;

import java.util.List;
import java.util.Optional;

/** 다른 모듈이 사용하는 CourseCatalog 계약. */
public interface CourseCatalog {

    Optional<CourseSummary> findByCourseKey(String courseKey);

    List<CourseSummary> findByCourseKeyIn(List<String> courseKeys);

    List<CourseName> findNamesByCourseKeyIn(List<String> courseKeys);

    boolean existsByCourseKey(String courseKey);
}
