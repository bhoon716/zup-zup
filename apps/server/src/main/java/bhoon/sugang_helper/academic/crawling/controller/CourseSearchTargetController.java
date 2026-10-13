package bhoon.sugang_helper.academic.crawling.controller;

import bhoon.sugang_helper.academic.crawling.dto.response.SearchDefaultSemesterResponse;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
import bhoon.sugang_helper.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseSearchTargetController {

    private final CourseCrawlerTargetService courseCrawlerTargetService;

    @GetMapping("/search-default-semester")
    public ResponseEntity<CommonResponse<SearchDefaultSemesterResponse>> getSearchDefaultSemester() {
        SearchDefaultSemesterResponse response = courseCrawlerTargetService.getSearchDefaultSemester();
        return CommonResponse.ok(response, "검색 기본 학기를 조회했습니다.");
    }
}
