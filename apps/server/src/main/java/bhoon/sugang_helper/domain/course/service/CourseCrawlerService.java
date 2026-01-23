package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlerService {

    private final CourseRepository courseRepository;
    private final JbnuCourseApiClient apiClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void crawlAndSaveCourses() {
        log.info("Starting course crawling...");
        try {
            String xmlResponse = apiClient.fetchCourseDataXml();
            List<Course> courses = parseCourses(xmlResponse);

            int savedCount = processCourses(courses);

            log.info("Crawling finished. Processed {} courses.", savedCount);
        } catch (IOException e) {
            log.error("Failed to fetch course data: {}", e.getMessage(), e);
            throw new RuntimeException("Crawler failed", e);
        }
    }

    private int processCourses(List<Course> courses) {
        int count = 0;
        for (Course crawledCourse : courses) {
            processAndSaveCourse(crawledCourse);
            count++;
        }
        return count;
    }

    private void processAndSaveCourse(Course crawledCourse) {
        courseRepository.findById(crawledCourse.getCourseKey())
                .ifPresentOrElse(
                        existingCourse -> updateExistingCourse(existingCourse, crawledCourse),
                        () -> courseRepository.save(crawledCourse));
    }

    private void updateExistingCourse(Course existingCourse, Course crawledCourse) {
        boolean wasFull = existingCourse.getAvailable() <= 0;
        existingCourse.updateStatus(crawledCourse.getCurrent(), crawledCourse.getAvailable());

        if (wasFull && existingCourse.getAvailable() > 0) {
            publishSeatOpenedEvent(existingCourse);
        }
    }

    private void publishSeatOpenedEvent(Course course) {
        log.info("Seat Opened! Course: {}, Available: {}", course.getName(), course.getAvailable());
        eventPublisher.publishEvent(new SeatOpenedEvent(
                course.getCourseKey(),
                course.getName(),
                0,
                course.getAvailable()));
    }

    private List<Course> parseCourses(String xmlData) {
        List<Course> courseList = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", Parser.xmlParser());

        Elements rows = doc.select("Dataset[id=GRD_COUR001] > Rows > Row");

        for (Element row : rows) {
            String sbjtCd = getColValue(row, "SBJTCD");
            String clss = getColValue(row, "CLSS");
            String sbjtNm = getColValue(row, "SBJTNM");
            String profNm = getColValue(row, "RPSTPROFNM");
            int lmtrCnt = safeParseInt(getColValue(row, "LMTRCNT"));
            int tlsnrCnt = safeParseInt(getColValue(row, "TLSNRCNT"));

            if (sbjtCd != null && clss != null) {
                Course course = Course.builder()
                        .courseKey(sbjtCd + "-" + clss)
                        .name(sbjtNm)
                        .professor(profNm)
                        .capacity(lmtrCnt)
                        .current(tlsnrCnt)
                        .build();
                courseList.add(course);
            }
        }
        return courseList;
    }

    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        return col != null ? col.text() : null;
    }

    private int safeParseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
