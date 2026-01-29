package bhoon.sugang_helper.domain.timetable.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import bhoon.sugang_helper.domain.timetable.entity.TimetableEntry;
import bhoon.sugang_helper.domain.timetable.repository.CustomScheduleRepository;
import bhoon.sugang_helper.domain.timetable.repository.TimetableEntryRepository;
import bhoon.sugang_helper.domain.timetable.repository.TimetableRepository;
import bhoon.sugang_helper.domain.timetable.request.CustomScheduleRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableRequest;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @InjectMocks
    private TimetableService timetableService;

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private CustomScheduleRepository customScheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private static MockedStatic<SecurityUtil> securityUtil;

    private User testUser;
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
        testUser = mock(User.class);
        given(testUser.getId()).willReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    private void mockUser() {
        given(SecurityUtil.getCurrentUserEmail()).willReturn(testEmail);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("시간표 생성 - 성공")
    void createTimetable_Success() {
        // given
        mockUser();
        TimetableRequest request = new TimetableRequest("My Timetable", false);
        given(timetableRepository.countByUserId(1L)).willReturn(0L);
        given(timetableRepository.save(any(Timetable.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = timetableService.createTimetable(request);

        // then
        assertThat(response.getName()).isEqualTo("My Timetable");
        verify(timetableRepository, times(1)).save(any(Timetable.class));
    }

    @Test
    @DisplayName("시간표 생성 - 10개 초과 시 예외 발생")
    void createTimetable_LimitExceeded_ThrowsException() {
        // given
        mockUser();
        TimetableRequest request = new TimetableRequest("My Timetable", false);
        given(timetableRepository.countByUserId(1L)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> timetableService.createTimetable(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_TIMETABLE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("강좌 추가 - 성공 (시간 겹침 없음)")
    void addCourse_Success() {
        // given
        mockUser();
        Timetable timetable = Timetable.builder().userId(1L).name("P1").build();
        given(timetableRepository.findById(1L)).willReturn(Optional.of(timetable));

        given(courseRepository.existsByCourseKey("CK1")).willReturn(true);
        given(timetableEntryRepository.findByTimetableIdAndCourseKey(1L, "CK1")).willReturn(Optional.empty());

        // when
        timetableService.addCourse(1L, "CK1");

        // then
        assertThat(timetable.getEntries()).hasSize(1);
    }

    @Test
    @DisplayName("강좌 추가 - 성공 (시간 겹침 허용)")
    void addCourse_OverlapAllowed_Success() {
        // given
        mockUser();
        Timetable timetable = Timetable.builder().userId(1L).name("P1").build();
        TimetableEntry existingEntry = TimetableEntry.builder().timetable(timetable).courseKey("EXISTING").build();
        timetable.addEntry(existingEntry);

        given(timetableRepository.findById(1L)).willReturn(Optional.of(timetable));
        given(courseRepository.existsByCourseKey("NEW")).willReturn(true);
        given(timetableEntryRepository.findByTimetableIdAndCourseKey(1L, "NEW")).willReturn(Optional.empty());

        // when
        timetableService.addCourse(1L, "NEW");

        // then
        assertThat(timetable.getEntries()).hasSize(2);
    }

    @Test
    @DisplayName("커스텀 일정 추가 - 강좌와 시간 겹침 허용")
    void addCustomSchedule_OverlapAllowed_Success() {
        // given
        mockUser();
        Timetable timetable = Timetable.builder().userId(1L).name("P1").build();
        given(timetableRepository.findById(1L)).willReturn(Optional.of(timetable));

        CustomScheduleRequest request = new CustomScheduleRequest("Lunch", "월",
                LocalTime.of(9, 45), LocalTime.of(10, 45), "#FFFFFF");

        // when
        timetableService.addCustomSchedule(1L, request);

        // then
        assertThat(timetable.getCustomSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("대표 시간표 설정 - 이전 대표 시간표 해제 확인")
    void setPrimary_ResetsPreviousPrimary() {
        // given
        mockUser();
        Timetable oldPrimary = Timetable.builder().userId(1L).name("Old").isPrimary(true).build();
        Timetable newPrimary = Timetable.builder().userId(1L).name("New").isPrimary(false).build();

        given(timetableRepository.findById(2L)).willReturn(Optional.of(newPrimary));
        given(timetableRepository.findByUserIdAndIsPrimaryTrue(1L)).willReturn(List.of(oldPrimary));

        // when
        timetableService.setPrimary(2L);

        // then
        assertThat(oldPrimary.isPrimary()).isFalse();
        assertThat(newPrimary.isPrimary()).isTrue();
    }
}
