package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.dto.ParsedCourseDto;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.enums.SemesterType;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.domain.course.response.CrawlTargetInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
    @DisplayName("여석 발생 감지: 0명에서 1명으로 변경 시 이벤트 발행 및 이력 저장")
    void detectSeatOpening_SavesHistoryAndPublishesEvent() throws IOException {
        // given
        setupMockCrawlData(List.of(createCourseDto("12345:01", 50, 49)));
        
        Course existingFullCourse = createCourse("12345:01", 50, 50);
        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingFullCourse));

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("여석 유지 시: 이미 여석이 있었던 경우 이벤트는 미발행하고 이력만 저장")
    void alreadyAvailable_SavesHistoryOnly() throws IOException {
        // given
        setupMockCrawlData(List.of(createCourseDto("12345:01", 50, 48)));
        
        Course existingAvailableCourse = createCourse("12345:01", 50, 49);
        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingAvailableCourse));

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(eventPublisher, never()).publishEvent(any(SeatOpenedEvent.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("새로운 강의 발견 시: 강의 정보를 저장하고 첫 이력을 기록")
    void newCourseFound_SavesCourseAndHistory() throws IOException {
        // given
        setupMockCrawlData(List.of(createCourseDto("NEW:01", 50, 40)));
        given(courseRepository.findByCourseKey("NEW:01")).willReturn(Optional.empty());

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("데이터 변경 없는 경우: 이력을 저장하지 않음")
    void noChange_DoesNotSaveHistory() throws IOException {
        // given
        setupMockCrawlData(List.of(createCourseDto("SAME:01", 50, 10)));
        
        Course existingCourse = createCourse("SAME:01", 50, 10);
        given(courseRepository.findByCourseKey("SAME:01")).willReturn(Optional.of(existingCourse));

        // when
        courseCrawlerService.crawlAndSaveCourses();

        // then
        verify(courseSeatHistoryRepository, never()).save(any(CourseSeatHistory.class));
    }

    private void setupMockCrawlData(List<ParsedCourseDto> dtoList) throws IOException {
        String mockXml = "<mock></mock>";
        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CrawlTargetInfo("2026", SemesterType.FIRST_SEMESTER));
        given(apiClient.fetchCourseDataXml("2026", SemesterType.FIRST_SEMESTER.getCode())).willReturn(mockXml);
        given(courseParser.parseCourses(mockXml)).willReturn(dtoList);
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

    private ParsedCourseDto createCourseDto(String courseKey, int capacity, int current) {
        return new ParsedCourseDto(
                courseKey, "12345", "테스트 강의", "01", "김교수",
                capacity, current, null, "2026", "U211600010",
                null, "컴퓨터공학부", null, "월 1-A", "3",
                null, null, null, 3, null,
                null, null, null, "7호관 101호", true,
                null, null, null, Collections.emptyList());
    }
}
