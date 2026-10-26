package bhoon.sugang_helper.academic.crawling.dto.result;

import bhoon.sugang_helper.academic.course.enums.SemesterType;




public record CrawlTargetInfo(String year, SemesterType semester) {
}
