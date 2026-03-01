package bhoon.sugang_helper.domain.course.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.course.request.AdminCrawlTargetRequest;
import bhoon.sugang_helper.domain.course.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import bhoon.sugang_helper.domain.course.service.CourseCrawlerTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin Course", description = "관리자 강의 관리 API")
public class AdminCourseController {

    private final CourseCrawlerService courseCrawlerService;
    private final CourseCrawlerTargetService crawlerTargetService;

    /**
     * 스케줄러 및 기본 수동 크롤링에 사용되는 현재 크롤링 타겟(년도, 학기)을 조회합니다.
     */
    @Operation(summary = "기본 크롤링 타겟 조회", description = "스케줄러/기본 수동 크롤링에 사용되는 년도/학기 타겟을 조회합니다.")
    @GetMapping("/crawl-target")
    public ResponseEntity<CommonResponse<AdminCrawlTargetResponse>> getCrawlTarget() {
        AdminCrawlTargetResponse response = crawlerTargetService.getCurrentTarget();
        return CommonResponse.ok(response, "기본 크롤링 타겟을 조회했습니다.");
    }

    /**
     * 스케줄러 및 기본 수동 크롤링에 사용할 년도와 학기 타겟 정보를 저장하거나 수정합니다.
     */
    @Operation(summary = "기본 크롤링 타겟 저장", description = "스케줄러/기본 수동 크롤링에 사용할 년도/학기 타겟을 저장합니다.")
    @PutMapping("/crawl-target")
    public ResponseEntity<CommonResponse<AdminCrawlTargetResponse>> updateCrawlTarget(
            @Valid @RequestBody AdminCrawlTargetRequest request) {
        AdminCrawlTargetResponse response = crawlerTargetService.updateTarget(request.getYear(), request.getSemester());
        return CommonResponse.ok(response, "기본 크롤링 타겟을 저장했습니다.");
    }

    /**
     * 현재 DB에 저장된 기본 크롤링 타겟으로 강의 데이터 수집 작업을 즉시 실행합니다.
     */
    @Operation(summary = "강의 크롤링 실행(기본 타겟)", description = "저장된 기본 년도/학기 타겟으로 강의 크롤링 작업을 즉시 실행합니다.")
    @PostMapping("/crawl")
    public ResponseEntity<CommonResponse<String>> crawlCourses() {
        courseCrawlerService.crawlAndSaveCourses();
        return CommonResponse.ok("강의 크롤링 작업을 실행했습니다.", "기본 타겟으로 강의 크롤링 작업을 실행했습니다.");
    }

    /**
     * 요청 바디에 포함된 특정 년도와 학기를 대상으로 강의 데이터 수집 작업을 즉시 실행합니다.
     */
    @Operation(summary = "강의 크롤링 실행(특정 타겟)", description = "요청한 년도/학기로 강의 크롤링 작업을 즉시 실행합니다.")
    @PostMapping("/crawl/target")
    public ResponseEntity<CommonResponse<String>> crawlCoursesByTarget(
            @Valid @RequestBody AdminCrawlTargetRequest request) {
        CourseCrawlerTargetService.CrawlTarget target = crawlerTargetService.normalizeTarget(request.getYear(),
                request.getSemester());
        courseCrawlerService.crawlAndSaveCourses(target.year(), target.semester());
        String message = String.format("년도 %s, 학기 %s 크롤링 작업을 실행했습니다.", target.year(), target.semester());
        return CommonResponse.ok(message, message);
    }
}
