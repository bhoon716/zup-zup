package bhoon.sugang_helper.academic.course.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import bhoon.sugang_helper.academic.course.dto.ParsedCourseDto;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.event.SeatOpenedEvent;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseCatalogSynchronizationServiceTest {

    private static final String COURSE_KEY = "2026:U211600010:COMP101:01";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSeatHistoryRepository courseSeatHistoryRepository;
    @Mock
    private SeatNotificationOutboxService outboxService;
    @Mock
    private MeterRegistry meterRegistry;
    @InjectMocks
    private CourseCatalogSynchronizationService synchronizationService;

    @Test
    @DisplayName("정규화 후 변경 없는 강의는 Course·시간표·좌석 이력 쓰기를 만들지 않는다")
    void unchangedCourse_DoesNotUseDomainSavePaths() {
        Course existing = createCourse(null, 50, 20);
        ParsedCourseDto crawled = createDto("   ", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of(existing));

        synchronizationService.synchronize(List.of(crawled));

        verify(courseRepository, never()).saveAll(any());
        verifyNoInteractions(courseSeatHistoryRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("새 강의는 강의와 최초 좌석 이력을 저장한다")
    void newCourse_SavesCourseAndSeatHistory() {
        ParsedCourseDto crawled = createDto("김교수", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of());

        synchronizationService.synchronize(List.of(crawled));

        verify(courseRepository).saveAll(any());
        verify(courseSeatHistoryRepository).saveAll(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("유효 허용 인원이 찬 과목은 전체 수강인원이 줄어도 여석 이벤트를 발행하지 않는다")
    void allowedEnrollmentFull_DoesNotPublishSeatOpenedEvent() {
        Course existing = createCourse("함운철", 60, 60);
        ParsedCourseDto crawled = createDto("함운철", 18, 59);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of(existing));

        synchronizationService.synchronize(List.of(crawled));

        verifyNoInteractions(outboxService);
        verify(courseSeatHistoryRepository).saveAll(any());
    }

    @Test
    @DisplayName("여석이 처음 열릴 때만 그 시점의 좌석 정보를 outbox에 전달한다")
    void seatOpening_EnqueuesOnlyTheFullToAvailableTransition() {
        // given
        Course existing = createCourse("김교수", 50, 50);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of(existing));

        // when
        synchronizationService.synchronize(List.of(createDto("김교수", 50, 49)));
        synchronizationService.synchronize(List.of(createDto("김교수", 50, 48)));

        // then
        verify(outboxService).enqueue(new SeatOpenedEvent(COURSE_KEY, "컴퓨터프로그래밍", "김교수", 0, 1));
        verifyNoMoreInteractions(outboxService);
    }

    @Test
    @DisplayName("outbox 저장 실패를 성공으로 삼키지 않고 호출자에게 전달한다")
    void outboxWriteFailure_PropagatesFailure() {
        // given
        Course existing = createCourse("김교수", 50, 50);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of(existing));
        IllegalStateException failure = new IllegalStateException("outbox write failed");
        doThrow(failure).when(outboxService).enqueue(any());

        // when & then
        assertThatThrownBy(() -> synchronizationService.synchronize(List.of(createDto("김교수", 50, 49))))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("도메인 저장 실패를 호출자에게 전달한다")
    void domainWriteFailure_PropagatesFailure() {
        ParsedCourseDto crawled = createDto("김교수", 50, 20);
        given(courseRepository.findByCourseKeyIn(List.of(COURSE_KEY))).willReturn(List.of());
        doThrow(new IllegalStateException("write failed")).when(courseRepository).saveAll(any());

        assertThatThrownBy(() -> synchronizationService.synchronize(List.of(crawled)))
                .isInstanceOf(IllegalStateException.class);
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
