package bhoon.sugang_helper.academic.course.dto.result;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import java.time.LocalDateTime;




public record CourseSearchContext(String year, SemesterType semester, LocalDateTime lastCrawledAt) {
}
