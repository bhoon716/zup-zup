package bhoon.sugang_helper.domain.course.controller;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin Course", description = "관리자 강의 관리 API")
public class AdminCourseController {

    private final CourseCrawlerService courseCrawlerService;

    @Operation(summary = "강의 크롤링 실행", description = "관리자 권한으로 강의 크롤링 작업을 즉시 실행합니다.")
    @PostMapping("/crawl")
    public ResponseEntity<String> crawlCourses() {
        courseCrawlerService.crawlAndSaveCourses();
        return ResponseEntity.ok("강의 크롤링 작업을 실행했습니다.");
    }
}
