package bhoon.sugang_helper.academic.course.dto.result;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import java.time.LocalDateTime;

/**
 * 강의 검색·상세 응답에 표시할 현재 대상 학기와 마지막 수집 시각입니다.
 */
public record CourseSearchContext(String year, SemesterType semester, LocalDateTime lastCrawledAt) {
}
