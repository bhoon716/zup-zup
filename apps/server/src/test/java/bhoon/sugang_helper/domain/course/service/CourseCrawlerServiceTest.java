package bhoon.sugang_helper.domain.course.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSeatHistory;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        private CourseCrawlerService courseCrawlerService;

        @BeforeEach
        void setUp() {
                courseCrawlerService = new CourseCrawlerService(
                                courseRepository, apiClient, courseParser, eventPublisher,
                                courseSeatHistoryRepository, crawlerTargetService, transactionManager);
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
                Course crawledCourse = createCourse("CK1", 50, 15); // 현재 인원 변경 (10 -> 15)
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
                Course crawledCourse = createCourse("CK1", 50, 10); // 동일한 인원 정보
                given(courseRepository.findByCourseKey("CK1")).willReturn(Optional.of(existingCourse));

                // when
                ReflectionTestUtils.invokeMethod(courseCrawlerService, "processCourse", crawledCourse);

                // then
                verify(courseSeatHistoryRepository, never()).save(any(CourseSeatHistory.class));
        }

        @Test
        @DisplayName("정원이 변경된 경우에도 이력을 저장한다")
        void processCourse_existingCourse_capacityChange_savesHistory() {
                // given
                Course existingCourse = createCourse("CK1", 50, 10);
                Course crawledCourse = createCourse("CK1", 60, 10); // 정원 변경 (50 -> 60)
                given(courseRepository.findByCourseKey("CK1")).willReturn(Optional.of(existingCourse));

                // when
                ReflectionTestUtils.invokeMethod(courseCrawlerService, "processCourse", crawledCourse);

                // then
                verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
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
