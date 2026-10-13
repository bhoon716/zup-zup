package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.ParsedCourseDto;
import java.util.List;

/**
 * 크롤링 경계가 수집한 강의 목록을 강의 카탈로그에 반영하도록 요청하는 계약입니다.
 */
public interface CourseCatalogSynchronizer {

    void synchronize(List<ParsedCourseDto> parsedCourses);
}
