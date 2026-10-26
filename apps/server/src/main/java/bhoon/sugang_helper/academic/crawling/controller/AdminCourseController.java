package bhoon.sugang_helper.academic.crawling.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.dto.request.AdminCrawlTargetRequest;
import bhoon.sugang_helper.academic.crawling.dto.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerService;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlerTargetService;
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
public class AdminCourseController {

    private final CourseCrawlerService courseCrawlerService;
    private final CourseCrawlerTargetService crawlerTargetService;

    /**
     * 스케줄러 및 기본 수동 크롤링에 사용되는 현재 크롤링 타겟(년도, 학기)을 조회합니다.
     */
    @GetMapping("/crawl-target")
    public ResponseEntity<AdminCrawlTargetResponse> getCrawlTarget() {
        AdminCrawlTargetResponse response = crawlerTargetService.getCurrentTarget();
        return ResponseEntity.ok(response);
    }

    /**
     * 스케줄러 및 기본 수동 크롤링에 사용할 년도와 학기 타겟 정보를 저장하거나 수정합니다.
     */
    @PutMapping("/crawl-target")
    public ResponseEntity<AdminCrawlTargetResponse> updateCrawlTarget(
            @Valid @RequestBody AdminCrawlTargetRequest request) {
        AdminCrawlTargetResponse response = crawlerTargetService.updateTarget(request.getYear(), request.getSemester());
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 DB에 저장된 기본 크롤링 타겟으로 강의 데이터 수집 작업을 즉시 실행합니다.
     */
    @PostMapping("/crawl")
    public ResponseEntity<JsonNode> crawlCourses() {
        courseCrawlerService.crawlAndSaveCourses();
        return ResponseEntity.ok(TextNode.valueOf("강의 크롤링 작업을 실행했습니다."));
    }

    /**
     * 요청 바디에 포함된 특정 년도와 학기를 대상으로 강의 데이터 수집 작업을 즉시 실행합니다.
     */
    @PostMapping("/crawl/target")
    public ResponseEntity<JsonNode> crawlCoursesByTarget(
            @Valid @RequestBody AdminCrawlTargetRequest request) {
        CrawlTargetInfo target = crawlerTargetService.normalizeTarget(request.getYear(),
                request.getSemester());
        courseCrawlerService.crawlAndSaveCourses(target.year(), target.semester().getCode());
        String message = String.format("년도 %s, 학기 %s 크롤링 작업을 실행했습니다.", target.year(),
                target.semester().getDescription());
        return ResponseEntity.ok(TextNode.valueOf(message));
    }
}
