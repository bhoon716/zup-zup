package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.result.CourseSearchContext;

/**
 * 강의 응답에 필요한 현재 수집 문맥을 제공합니다.
 */
public interface CourseSearchContextProvider {

    CourseSearchContext getCurrentContext();
}
