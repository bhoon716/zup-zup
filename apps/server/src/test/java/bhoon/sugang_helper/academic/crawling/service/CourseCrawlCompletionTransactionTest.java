package bhoon.sugang_helper.academic.crawling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.academic.course.service.CourseCatalogSynchronizationService;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerStatusRepository;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CourseCatalogSynchronizationService.class, CourseCrawlCompletionService.class,
        CrawlerRunStateService.class, SeatNotificationOutboxService.class})
class CourseCrawlCompletionTransactionTest {

    private static final String COURSE_KEY = "2026:U211600010:COMP101:01";
    private static final String COURSE_NAME = "컴퓨터프로그래밍";

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CourseCrawlCompletionService completionService;
    @Autowired
    private SeatNotificationOutboxService outboxService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseSeatHistoryRepository courseSeatHistoryRepository;
    @Autowired
    private CrawlerStatusRepository crawlerStatusRepository;
    @MockitoSpyBean
    private SeatNotificationOutboxRepository outboxRepository;

    @MockitoBean
    private MeterRegistry meterRegistry;
    @MockitoBean
    private SlackAlertService slackAlertService;

    @BeforeEach
    void clearData() {
        outboxRepository.findAll().forEach(outboxRepository::delete);
        courseSeatHistoryRepository.findAll().forEach(courseSeatHistoryRepository::delete);
        courseRepository.findAll().forEach(courseRepository::delete);
        crawlerStatusRepository.findAll().forEach(crawlerStatusRepository::delete);
    }

    @Test
    @DisplayName("강의 반영, 좌석 이력, 크롤링 성공 상태와 알림 outbox가 함께 커밋된다")
    void commitsCatalogStateCrawlerStatusAndNotificationOutboxTogether() {
        courseRepository.saveAndFlush(fullCourse());

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                completionService.complete(List.of(parsedCourseWithAvailableSeat())));

        Course updated = courseRepository.findByCourseKey(COURSE_KEY).orElseThrow();
        assertThat(updated.getCurrent()).isEqualTo(59);
        assertThat(courseSeatHistoryRepository.count()).isEqualTo(1);
        assertThat(crawlerStatusRepository.findById(1L))
                .hasValueSatisfying(status -> assertThat(status.getLastSuccessAt()).isNotNull());
        assertThat(outboxRepository.findAll()).singleElement().satisfies(outbox -> {
            assertThat(outbox.getCourseKey()).isEqualTo(COURSE_KEY);
            assertThat(outbox.getCourseName()).isEqualTo(COURSE_NAME);
            assertThat(outbox.getProfessor()).isEqualTo("김교수");
            assertThat(outbox.getPreviousSeats()).isZero();
            assertThat(outbox.getCurrentSeats()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("동기화 트랜잭션이 롤백되면 강의, 좌석 이력, 성공 상태와 알림 outbox가 남지 않는다")
    void rollsBackCatalogStateCrawlerStatusAndNotificationOutboxTogether() {
        courseRepository.saveAndFlush(fullCourse());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            completionService.complete(List.of(parsedCourseWithAvailableSeat()));
            status.setRollbackOnly();
        });

        Course unchanged = courseRepository.findByCourseKey(COURSE_KEY).orElseThrow();
        assertThat(unchanged.getCurrent()).isEqualTo(60);
        assertThat(courseSeatHistoryRepository.count()).isZero();
        assertThat(crawlerStatusRepository.findById(1L)).isEmpty();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    @DisplayName("다음 청크의 outbox 저장이 실패하면 앞 청크에 저장한 강의와 좌석 이력도 롤백한다")
    void outboxFailureRollsBackPreviouslyWrittenChunksAndCrawlerSuccess() {
        // given
        courseRepository.saveAndFlush(fullCourse());
        List<ParsedCourseDto> courses = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            courses.add(parsedCourse("new-course-" + index, 59));
        }
        courses.add(parsedCourseWithAvailableSeat());
        doThrow(new IllegalStateException("outbox write failed")).when(outboxRepository).save(any());

        // when & then
        assertThatThrownBy(() -> completionService.complete(courses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox write failed");
        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findByCourseKey(COURSE_KEY).orElseThrow().getCurrent()).isEqualTo(60);
        assertThat(courseSeatHistoryRepository.count()).isZero();
        assertThat(crawlerStatusRepository.findById(1L)).isEmpty();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void rejectsEnqueueOutsideTheCatalogTransaction() {
        // given
        SeatOpenedEvent event = new SeatOpenedEvent(COURSE_KEY, COURSE_NAME, "김교수", 0, 1);

        // when & then
        assertThatThrownBy(() -> outboxService.enqueue(event))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(outboxRepository.count()).isZero();
    }

    private Course fullCourse() {
        return Course.builder()
                .courseKey(COURSE_KEY)
                .subjectCode("COMP101")
                .name(COURSE_NAME)
                .classNumber("01")
                .professor("김교수")
                .capacity(60)
                .current(60)
                .academicYear("2026")
                .semester("U211600010")
                .build();
    }

    private ParsedCourseDto parsedCourseWithAvailableSeat() {
        return parsedCourse(COURSE_KEY, 59);
    }

    private ParsedCourseDto parsedCourse(String courseKey, int current) {
        return new ParsedCourseDto(
                courseKey,
                "COMP101",
                null,
                COURSE_NAME,
                "01",
                "김교수",
                60,
                current,
                null,
                "2026",
                "U211600010",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }
}
