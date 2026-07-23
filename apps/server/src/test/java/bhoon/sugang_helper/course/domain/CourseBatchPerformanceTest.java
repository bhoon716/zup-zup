package bhoon.sugang_helper.course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@Tag("performance")
class CourseBatchPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(CourseBatchPerformanceTest.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager em;

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        sessionFactory.getStatistics().clear();
    }

    @Test
    @DisplayName("변경 없는 강의 1,000개 배치 업데이트 시 Entity Update 발생 0건 및 속도 비교")
    void measureUnchangedCoursesUpdatePerformance() {
        int courseCount = 1000;
        List<Course> existingCourses = new ArrayList<>();

        for (int i = 0; i < courseCount; i++) {
            Course course = Course.builder()
                    .courseKey("BATCH-PERF-" + i)
                    .subjectCode("CSE-PERF-" + i)
                    .name("배치성능테스트과목" + i)
                    .classNumber("01")
                    .professor("김교수")
                    .capacity(50)
                    .current(20)
                    .academicYear("2026")
                    .semester("U211600010")
                    .build();
            course.addSchedule(new CourseSchedule(CourseDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
            existingCourses.add(course);
        }
        courseRepository.saveAll(existingCourses);
        em.flush();
        em.clear();

        sessionFactory.getStatistics().clear();

        long startTime = System.nanoTime();

        for (int i = 0; i < courseCount; i++) {
            Course existingCourse = courseRepository.findByCourseKey("BATCH-PERF-" + i).orElseThrow();

            Course crawledCourse = Course.builder()
                    .courseKey("BATCH-PERF-" + i)
                    .subjectCode("CSE-PERF-" + i)
                    .name("배치성능테스트과목" + i)
                    .classNumber("01")
                    .professor("김교수")
                    .capacity(50)
                    .current(20)
                    .academicYear("2026")
                    .semester("U211600010")
                    .build();
            crawledCourse.addSchedule(new CourseSchedule(CourseDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));

            existingCourse.updateMetadata(crawledCourse);
        }
        em.flush();

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        Statistics stats = sessionFactory.getStatistics();
        long updateCount = stats.getEntityUpdateCount();

        log.info("==================================================================");
        log.info("크롤러 배치 성능 측정 결과 (변경 없는 강의 {}개)", courseCount);
        log.info("소요 시간: {} ms", String.format("%.2f", durationMs));
        log.info("발생한 Entity Update 쿼리 수: {}", updateCount);
        log.info("개선 전 예상 Entity Update 쿼리 수: {}", courseCount);
        log.info("==================================================================");

        assertThat(updateCount).isZero();
    }
}
