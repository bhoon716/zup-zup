package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

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
        private CourseCrawlerService crawlerService;

        /**
         * 신규 강의 데이터 크롤링 시 DB 저장 및 이력 기록 검증
         */
        @Test
        @DisplayName("새로운 강좌 데이터를 성공적으로 크롤링하여 저장한다")
        void crawlAndSave_NewCourses() throws java.io.IOException {
                // given
                String xmlResponse = "<xml>...</xml>";
                Course course = Course.builder().courseKey("12345-01").name("Test Course").capacity(40).current(30)
                                .build();

                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
                given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(xmlResponse);
                given(courseParser.parseCourses(xmlResponse)).willReturn(List.of(course));
                given(courseRepository.findByCourseKey("12345-01")).willReturn(Optional.empty());

                // when
                crawlerService.crawlAndSaveCourses();

                // then
                verify(courseRepository, times(1)).save(any(Course.class));
                verify(courseSeatHistoryRepository, times(1)).save(any());
                verify(eventPublisher, never()).publishEvent(any());
        }

        /**
         * 기존 강의 업데이트 시 빈자리 발생 여부를 판단하고 이벤트를 발행하는지 검증
         */
        @Test
        @DisplayName("기존 강좌의 빈자리가 생기면 알림 이벤트를 발행한다")
        void crawlAndSave_UpdateWithEvent() throws java.io.IOException {
                // given
                Course existingCourse = Course.builder().courseKey("12345-01").name("Test Course").capacity(40)
                                .current(40)
                                .build();
                Course crawledCourse = Course.builder().courseKey("12345-01").name("Test Course").capacity(40)
                                .current(35)
                                .build();
                String xmlResponse = "<xml>...</xml>";

                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
                given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(xmlResponse);
                given(courseParser.parseCourses(xmlResponse)).willReturn(List.of(crawledCourse));
                given(courseRepository.findByCourseKey("12345-01")).willReturn(Optional.of(existingCourse));

                // when
                crawlerService.crawlAndSaveCourses();

                // then
                verify(courseRepository, times(1)).save(existingCourse);
                verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
                assertThat(existingCourse.getAvailable()).isEqualTo(5);
        }

        /**
         * 클라이언트 연결 실패 시 커스텀 예외(CRAWLER_CONNECTION_ERROR) 발생 여부 검증
         */
        @Test
        @DisplayName("API 호출 실패 시 Connection Error 예외를 발생시킨다")
        void crawlAndSave_ConnectionError() throws java.io.IOException {
                // given
                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
                given(apiClient.fetchCourseDataXml("2026", "U211600010"))
                                .willThrow(new RuntimeException("Network down"));

                // when & then
                assertThatThrownBy(() -> crawlerService.crawlAndSaveCourses())
                                .isInstanceOf(bhoon.sugang_helper.common.error.CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode",
                                                bhoon.sugang_helper.common.error.ErrorCode.CRAWLER_CONNECTION_ERROR);
        }

        /**
         * 크롤링된 데이터가 없을 경우 커스텀 예외(CRAWLER_NO_DATA) 발생 여부 검증
         */
        @Test
        @DisplayName("파싱된 데이터가 없으면 No Data 예외를 발생시킨다")
        void crawlAndSave_NoData() throws java.io.IOException {
                // given
                String xmlResponse = "<xml>...</xml>";
                given(crawlerTargetService.getCurrentTargetValue())
                                .willReturn(new CourseCrawlerTargetService.CrawlTarget("2026", "U211600010"));
                given(apiClient.fetchCourseDataXml("2026", "U211600010")).willReturn(xmlResponse);
                given(courseParser.parseCourses(xmlResponse)).willReturn(List.of());

                // when & then
                assertThatThrownBy(() -> crawlerService.crawlAndSaveCourses())
                                .isInstanceOf(bhoon.sugang_helper.common.error.CustomException.class)
                                .hasFieldOrPropertyWithValue("errorCode",
                                                bhoon.sugang_helper.common.error.ErrorCode.CRAWLER_NO_DATA);
        }

        @Test
        @Tag("manual")
        @DisplayName("실제 크롤링 및 저장 진행")
        void crawlAndSave_RealIntegration() {
                ReflectionTestUtils.setField(apiClient, "apiUrl", "http://example.com");
        }
}
