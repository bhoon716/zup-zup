package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private JbnuCourseApiClient apiClient;
    @Mock
    private JbnuCourseParser courseParser;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CourseSeatHistoryRepository courseSeatHistoryRepository;
    @Mock
    private CourseCrawlerTargetService crawlerTargetService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("여석 발생 감지: 0명에서 1명으로 변경 시 이벤트 발행")
    void detectSeatOpening() {
        // Given
        String mockXml = "<Root><Dataset id='GRD_COUR001'><Rows><Row><Col id='SBJTCD'>12345</Col></Row></Rows></Dataset></Root>";
        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
        given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(mockXml);

        Course crawledCourse = createCourse("12345:01", 50, 49);
        given(courseParser.parseCourses(mockXml)).willReturn(List.of(crawledCourse));

        Course existingCourse = createCourse("12345:01", 50, 50);
        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
    }

    @Test
    @DisplayName("여석 유지 시 무시: 이미 여석이 있었던 경우 이벤트 발행 안함")
    void noEventIfAlreadyAvailable() {
        // Given
        String mockXml = "<Root><Dataset id='GRD_COUR001'></Dataset></Root>";
        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
        given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(mockXml);

        Course crawledCourse = createCourse("12345:01", 50, 48);
        given(courseParser.parseCourses(mockXml)).willReturn(List.of(crawledCourse));

        Course existingCourse = createCourse("12345:01", 50, 49);
        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        verify(eventPublisher, never()).publishEvent(any(SeatOpenedEvent.class));
    }

    @Test
    @DisplayName("완전히 새로운 강의는 첫 크롤링 시 반드시 이력을 저장한다")
    void processCourse_newCourse_savesHistory() {
        // given
        Course crawledCourse = createCourse("CK1", 50, 10);
        given(courseRepository.findByCourseKey("CK1")).willReturn(Optional.empty());

        // when
        ReflectionTestUtils.invokeMethod(courseCrawlerService, "processCourse", crawledCourse);

        // then
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("기존 강의의 인원이 변경된 경우 이력을 저장한다")
    void processCourse_existingCourse_withChange_savesHistory() {
        // given
        Course existingCourse = createCourse("CK1", 50, 10);
        Course crawledCourse = createCourse("CK1", 50, 15);
        given(courseRepository.findByCourseKey("CK1")).willReturn(Optional.of(existingCourse));

        // when
        ReflectionTestUtils.invokeMethod(courseCrawlerService, "processCourse", crawledCourse);

        // then
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("기존 강의의 인원 변경이 없는 경우 이력을 저장하지 않는다")
    void processCourse_existingCourse_noChange_doesNotSaveHistory() {
        // given
        Course existingCourse = createCourse("CK1", 50, 10);
        Course crawledCourse = createCourse("CK1", 50, 10);
        given(courseRepository.findByCourseKey("CK1")).willReturn(Optional.of(existingCourse));

        // when
        ReflectionTestUtils.invokeMethod(courseCrawlerService, "processCourse", crawledCourse);

        // then
        verify(courseSeatHistoryRepository, never()).save(any(CourseSeatHistory.class));
    }

    private Course createCourse(String courseKey, int capacity, int current) {
        return Course.builder()
                .courseKey(courseKey)
                .name("테스트 강의")
                .capacity(capacity)
                .current(current)
                .academicYear("2026")
                .semester("U211600010")
                .build();
    }
}
