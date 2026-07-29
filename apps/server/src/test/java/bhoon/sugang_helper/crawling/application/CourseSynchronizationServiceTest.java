package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.infra.CourseSeatHistoryJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class CourseSynchronizationServiceTest {

    private static final String COURSE_KEY = "2026:U211600010:COMP101:01";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSeatHistoryJpaRepository courseSeatHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private CrawlerRunStateService crawlerRunStateService;

    @InjectMocks
    private CourseSynchronizationService synchronizationService;

    @Test
    @DisplayName("정규화 후 변경 없는 강의는 Course·시간표·좌석 이력 쓰기를 만들지 않는다")
    void unchangedCourse_DoesNotUseDomainSavePaths() {
        Course existing = createCourse(null, 50, 20);
        ParsedCourseDto crawled = createDto("   ", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of(existing));

        synchronizationService.synchronize(List.of(crawled));

        verify(courseRepository, never()).saveAll(any());
        verifyNoInteractions(courseSeatHistoryRepository);
        verifyNoInteractions(eventPublisher);
        verify(crawlerRunStateService).markSuccess();
    }

    @Test
    @DisplayName("새 강의는 강의와 최초 좌석 이력을 저장한다")
    void newCourse_SavesCourseAndSeatHistory() {
        ParsedCourseDto crawled = createDto("김교수", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of());

        synchronizationService.synchronize(List.of(crawled));

        verify(courseRepository).saveAll(any());
        verify(courseSeatHistoryRepository).saveAll(any());
        verify(crawlerRunStateService).markSuccess();
    }

    @Test
    @DisplayName("도메인 저장 실패 시 성공 상태를 기록하지 않는다")
    void domainWriteFailure_DoesNotMarkSuccess() {
        ParsedCourseDto crawled = createDto("김교수", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of());
        doThrow(new IllegalStateException("write failed")).when(courseRepository).saveAll(any());

        assertThatThrownBy(() -> synchronizationService.synchronize(List.of(crawled)))
                .isInstanceOf(IllegalStateException.class);
        verify(crawlerRunStateService, never()).markSuccess();
    }

    @Test
    @DisplayName("강의·시간표·좌석 이력·성공 상태 동기화는 하나의 트랜잭션 경계를 사용한다")
    void synchronize_DefinesSingleTransactionBoundary() throws NoSuchMethodException {
        Transactional transactional = CourseSynchronizationService.class
                .getMethod("synchronize", List.class)
                .getAnnotation(Transactional.class);

        org.assertj.core.api.Assertions.assertThat(transactional).isNotNull();
    }

    private Course createCourse(String professor, int capacity, int current) {
        return Course.builder()
                .courseKey(COURSE_KEY)
                .subjectCode("COMP101")
                .name("컴퓨터프로그래밍")
                .classNumber("01")
                .professor(professor)
                .capacity(capacity)
                .current(current)
                .academicYear("2026")
                .semester("U211600010")
                .build();
    }

    private ParsedCourseDto createDto(String professor, int capacity, int current) {
        return new ParsedCourseDto(
                COURSE_KEY,
                "COMP101",
                null,
                "컴퓨터프로그래밍",
                "01",
                professor,
                capacity,
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
