package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerServiceLogicTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private JbnuCourseApiClient apiClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseCrawlerService courseCrawlerService;

    @Test
    @DisplayName("Detect Seat Opening: 0 -> 1 seats triggers event")
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

        given(apiClient.fetchCourseDataXml()).willReturn(mockXml);

        // Existing course in DB has 0 available seats
        Course existingCourse = Course.builder()
                .courseKey("12345-01")
                .name("Test Course")
                .professor("Prof. Test")
                .capacity(50)
                .current(50) // Full
                .build();

        given(courseRepository.findById("12345-01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        // 1. Verify status updated
        // 2. Verify event published
        verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
    }

    @Test
    @DisplayName("No Event if seats were already available")
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

        given(apiClient.fetchCourseDataXml()).willReturn(mockXml);

        // Existing course already had 1 seat open
        Course existingCourse = Course.builder()
                .courseKey("12345-01")
                .capacity(50)
                .current(49) // 1 available
                .build();

        given(courseRepository.findById("12345-01")).willReturn(Optional.of(existingCourse));

        // When
        courseCrawlerService.crawlAndSaveCourses();

        // Then
        // Verify event NOT published
        verify(eventPublisher, never()).publishEvent(any(SeatOpenedEvent.class));
    }
}
