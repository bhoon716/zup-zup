package bhoon.sugang_helper.crawling.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.CourseSeatHistory;
import bhoon.sugang_helper.course.domain.CourseSeatHistoryRepository;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SpringBatchConfigTest {

    private static final String COURSE_KEY = "12345:01";
    private static final String NEW_COURSE_KEY = "NEW:01";
    private static final String SAME_COURSE_KEY = "SAME:01";
    private static final String ACADEMIC_YEAR = "2026";
    private static final String SEMESTER_CODE = "U211600010";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSeatHistoryRepository courseSeatHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SpringBatchConfig springBatchConfig;

    @Test
    @DisplayName("여석 발생 감지: 0명에서 1명으로 변경 시 이벤트 발행 및 이력 저장")
    void detectSeatOpening_SavesHistoryAndPublishesEvent() throws Exception {
        // given
        ParsedCourseDto dto = createCourseDto(COURSE_KEY, 50, 49);
        Course existingFullCourse = createCourse(COURSE_KEY, 50, 50);
        given(courseRepository.findByCourseKey(COURSE_KEY)).willReturn(Optional.of(existingFullCourse));

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when
        writer.write(new Chunk<>(Collections.singletonList(dto)));

        // then
        verify(eventPublisher, times(1)).publishEvent(any(SeatOpenedEvent.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("여석 유지 시: 이미 여석이 있었던 경우 이벤트는 미발행하고 이력만 저장")
    void alreadyAvailable_SavesHistoryOnly() throws Exception {
        // given
        ParsedCourseDto dto = createCourseDto(COURSE_KEY, 50, 48);
        Course existingAvailableCourse = createCourse(COURSE_KEY, 50, 49);
        given(courseRepository.findByCourseKey(COURSE_KEY)).willReturn(Optional.of(existingAvailableCourse));

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when
        writer.write(new Chunk<>(Collections.singletonList(dto)));

        // then
        verify(eventPublisher, never()).publishEvent(any(SeatOpenedEvent.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("새로운 강의 발견 시: 강의 정보를 저장하고 첫 이력을 기록")
    void newCourseFound_SavesCourseAndHistory() throws Exception {
        // given
        ParsedCourseDto dto = createCourseDto(NEW_COURSE_KEY, 50, 40);
        given(courseRepository.findByCourseKey(NEW_COURSE_KEY)).willReturn(Optional.empty());

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when
        writer.write(new Chunk<>(Collections.singletonList(dto)));

        // then
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("데이터 변경 없는 경우: 이력을 저장하지 않음")
    void noChange_DoesNotSaveHistory() throws Exception {
        // given
        ParsedCourseDto dto = createCourseDto(SAME_COURSE_KEY, 50, 10);
        Course existingCourse = createCourse(SAME_COURSE_KEY, 50, 10);
        given(courseRepository.findByCourseKey(SAME_COURSE_KEY)).willReturn(Optional.of(existingCourse));

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when
        writer.write(new Chunk<>(Collections.singletonList(dto)));

        // then
        verify(courseSeatHistoryRepository, never()).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("기존 강의 갱신 시에는 불필요한 save 호출 없이 변경 내용과 이력만 반영한다")
    void existingCourseUpdate_DoesNotCallSave() throws Exception {
        // given
        ParsedCourseDto dto = createCourseDto(SAME_COURSE_KEY, 60, 12);
        Course existingCourse = createCourse(SAME_COURSE_KEY, 50, 10);
        given(courseRepository.findByCourseKey(SAME_COURSE_KEY)).willReturn(Optional.of(existingCourse));

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when
        writer.write(new Chunk<>(Collections.singletonList(dto)));

        // then
        verify(courseRepository, never()).save(any(Course.class));
        verify(courseSeatHistoryRepository, times(1)).save(any(CourseSeatHistory.class));
    }

    @Test
    @DisplayName("과목 저장 실패 시 writer가 예외를 삼키지 않고 전파한다")
    void newCourseSaveFailure_IsPropagated() {
        // given
        ParsedCourseDto dto = createCourseDto(NEW_COURSE_KEY, 50, 40);
        given(courseRepository.findByCourseKey(NEW_COURSE_KEY)).willReturn(Optional.empty());
        doThrow(new RuntimeException("save failed")).when(courseRepository).save(any(Course.class));

        ItemWriter<ParsedCourseDto> writer = springBatchConfig.crawlWriter();

        // when / then
        assertThatThrownBy(() -> writer.write(new Chunk<>(Collections.singletonList(dto))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("save failed");
        verify(courseSeatHistoryRepository, never()).save(any(CourseSeatHistory.class));
    }

    private Course createCourse(String courseKey, int capacity, int current) {
        return Course.builder()
                .courseKey(courseKey)
                .name("테스트 강의")
                .capacity(capacity)
                .current(current)
                .academicYear(ACADEMIC_YEAR)
                .semester(SEMESTER_CODE)
                .build();
    }

    private ParsedCourseDto createCourseDto(String courseKey, int capacity, int current) {
        return new ParsedCourseDto(
                courseKey, "12345", "테스트 강의", "01", "김교수",
                capacity, current, null, ACADEMIC_YEAR, SEMESTER_CODE,
                null, "컴퓨터공학부", null, "월 1-A", "3",
                null, null, null, 3, null,
                null, null, null, "7호관 101호", true,
                null, null, null, Collections.emptyList());
    }
}
