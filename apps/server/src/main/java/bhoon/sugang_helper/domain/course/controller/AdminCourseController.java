package bhoon.sugang_helper.domain.course.controller;

import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseCrawlerService courseCrawlerService;

    @PostMapping("/crawl")
    public ResponseEntity<String> crawlCourses() {
        courseCrawlerService.crawlAndSaveCourses();
        return ResponseEntity.ok("Course crawling triggered successfully.");
    }
}
