package bhoon.sugang_helper.academic.crawling.presentation;

import bhoon.sugang_helper.academic.crawling.application.CourseCrawlerTargetService;
import bhoon.sugang_helper.academic.crawling.application.SearchDefaultSemesterResponse;
import bhoon.sugang_helper.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "과목 관련 API")
public class CourseSearchTargetController {

    private final CourseCrawlerTargetService courseCrawlerTargetService;

    @Operation(summary = "검색 기본 학기 조회", description = "강의 검색 페이지에서 사용할 기본 학기를 조회합니다.")
    @GetMapping("/search-default-semester")
    public ResponseEntity<CommonResponse<SearchDefaultSemesterResponse>> getSearchDefaultSemester() {
        SearchDefaultSemesterResponse response = courseCrawlerTargetService.getSearchDefaultSemester();
        return CommonResponse.ok(response, "검색 기본 학기를 조회했습니다.");
    }
}
