package bhoon.sugang_helper.academic.crawling.dto;

import bhoon.sugang_helper.academic.course.enums.SemesterType;

/**
 * 크롤링 타겟 정보를 담는 정보 객체입니다.
 */
public record CrawlTargetInfo(String year, SemesterType semester) {
}
