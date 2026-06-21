package bhoon.sugang_helper.course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
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
class CourseSchedulePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(CourseSchedulePerformanceTest.class);

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
    @DisplayName("동일한 시간표로 업데이트를 수행할 때 발생하는 SQL DELETE 및 INSERT 횟수를 측정한다")
    void measureQueryCountOnIdenticalSchedulesUpdate() {
        // 1. 기존 강의 데이터 생성 (3개의 시간표 포함)
        Course course = Course.builder()
                .courseKey("PERF-01")
                .subjectCode("CSE-PERF")
                .name("성능측정용과목")
                .classNumber("1")
                .professor("홍길동")
                .capacity(50)
                .current(10)
                .academicYear("2026")
                .semester("U211600010")
                .build();

        course.addSchedule(new CourseSchedule(CourseDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        course.addSchedule(new CourseSchedule(CourseDayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        course.addSchedule(new CourseSchedule(CourseDayOfWeek.FRIDAY, LocalTime.of(15, 0), LocalTime.of(16, 0)));

        courseRepository.save(course);
        em.flush();
        em.clear();

        // Hibernate 통계 데이터 초기화
        sessionFactory.getStatistics().clear();

        // 2. 저장된 강의를 다시 로드
        Course existingCourse = courseRepository.findByCourseKey("PERF-01").orElseThrow();

        // 3. 완전히 동일한 3개의 시간표 정보를 가지는 크롤링 결과 더미 데이터 생성
        Course crawledCourse = Course.builder()
                .courseKey("PERF-01")
                .subjectCode("CSE-PERF")
                .name("성능측정용과목")
                .classNumber("1")
                .professor("홍길동")
                .capacity(50)
                .current(10)
                .academicYear("2026")
                .semester("U211600010")
                .build();

        crawledCourse.addSchedule(new CourseSchedule(CourseDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        crawledCourse.addSchedule(new CourseSchedule(CourseDayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        crawledCourse.addSchedule(new CourseSchedule(CourseDayOfWeek.FRIDAY, LocalTime.of(15, 0), LocalTime.of(16, 0)));

        // 4. 성능 측정 시작
        long startTime = System.nanoTime();

        existingCourse.updateMetadata(crawledCourse);
        em.flush(); // DB에 반영

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        Statistics stats = sessionFactory.getStatistics();
        long deleteCount = stats.getEntityDeleteCount();
        long insertCount = stats.getEntityInsertCount();

        log.info("==================================================================");
        log.info("성능 측정 결과 (개선 전)");
        log.info("소요 시간: {} ms", String.format("%.2f", durationMs));
        log.info("Entity Delete 횟수: {}", deleteCount);
        log.info("Entity Insert 횟수: {}", insertCount);
        log.info("==================================================================");

        // 개선 전에는 3회의 delete와 3회의 insert가 발생함을 단언
        // (최적화 후에는 이 단언문이 0으로 변경되어야 함)
        assertThat(deleteCount).isEqualTo(3);
        assertThat(insertCount).isEqualTo(3);
    }
}
