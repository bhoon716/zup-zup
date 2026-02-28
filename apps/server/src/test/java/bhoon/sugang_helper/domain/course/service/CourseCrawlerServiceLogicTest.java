package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.io.IOException;
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
class CourseCrawlerServiceLogicTest {

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

    /**
     * 강의 여석이 0에서 1로 변할 때 알림 이벤트가 정상적으로 발행되는지 검증
     */
    @Test
    @DisplayName("여석 발생 감지: 0명에서 1명으로 변경 시 이벤트 발행")
    void detectSeatOpening() throws IOException {
        // Given
        String mockXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                    <Dataset id="GRD_COUR001">
                        <Rows>
                            <Row>
                                <Col id="SBJTCD">12345</Col>
                                <Col id="CLSS">01</Col>
                                <Col id="SBJTNM">Test Course</Col>
                                <Col id="RPSTPROFNM">Prof. Test</Col>
                                <Col id="LMTRCNT">50</Col>
                                <Col id="TLSNRCNT">49</Col> <!-- 1 seat available -->
                            </Row>
                        </Rows>
                    </Dataset>
                </Root>
                """;

        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
        given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(mockXml);

        Course crawledCourse = Course.builder()
                .courseKey("12345:01")
                .subjectCode("12345")
                .classNumber("01")
                .name("Test Course")
                .professor("Prof. Test")
                .capacity(50)
                .current(49)
                .build();
        given(courseParser.parseCourses(mockXml)).willReturn(List.of(crawledCourse));

        Course existingCourse = Course.builder()
                .courseKey("12345:01")
                .subjectCode("12345")
                .classNumber("01")
                .name("Test Course")
                .professor("Prof. Test")
                .capacity(50)
                .current(50)
                .build();

        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
    }

    /**
     * 이미 여석이 있었던 경우 인원수가 변하더라도 추가 알림 이벤트가 발행되지 않는지 검증
     */
    @Test
    @DisplayName("여석 유지 시 무시: 이미 여석이 있었던 경우 이벤트 발행 안함")
    void noEventIfAlreadyAvailable() throws IOException {
        // Given
        String mockXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                    <Dataset id="GRD_COUR001">
                        <Rows>
                            <Row>
                                <Col id="SBJTCD">12345</Col>
                                <Col id="CLSS">01</Col>
                                <Col id="LMTRCNT">50</Col>
                                <Col id="TLSNRCNT">48</Col> <!-- 2 seats available -->
                            </Row>
                        </Rows>
                    </Dataset>
                </Root>
                """;

        given(crawlerTargetService.getCurrentTargetValue())
                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
        given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(mockXml);

        Course crawledCourse = Course.builder()
                .courseKey("12345:01")
                .subjectCode("12345")
                .classNumber("01")
                .capacity(50)
                .current(48)
                .build();
        given(courseParser.parseCourses(mockXml)).willReturn(List.of(crawledCourse));

        Course existingCourse = Course.builder()
                .courseKey("12345:01")
                .subjectCode("12345")
                .classNumber("01")
                .capacity(50)
                .current(49)
                .build();

        given(courseRepository.findByCourseKey("12345:01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        verify(eventPublisher, never()).publishEvent(any(SeatOpenedEvent.class));
    }
}
