package bhoon.sugang_helper.dday.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.dday.domain.DdaySetting;
import bhoon.sugang_helper.dday.domain.DdaySettingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DdaySettingServiceTest {

    @InjectMocks
    private DdaySettingService ddaySettingService;

    @Mock
    private DdaySettingRepository ddaySettingRepository;

    @Test
    @DisplayName("가장 가까운 미래의 활성 D-Day 하나만 반환한다")
    void getActiveDday() {
        // given
        LocalDate today = LocalDate.now();
        DdaySetting d1 = DdaySetting.builder()
                .title("1학기 종강")
                .targetDate(today.plusDays(3))
                .targetTime(LocalTime.of(18, 0))
                .build();
        ReflectionTestUtils.setField(d1, "id", 1L);

        DdaySetting d2 = DdaySetting.builder()
                .title("2학기 종강")
                .targetDate(today.plusDays(100))
                .targetTime(LocalTime.of(18, 0))
                .build();
        ReflectionTestUtils.setField(d2, "id", 2L);

        when(ddaySettingRepository.findActiveDdays(today)).thenReturn(List.of(d1, d2));

        // when
        Optional<DdaySettingResponse> activeDday = ddaySettingService.getActiveDday();

        // then
        assertThat(activeDday).isPresent();
        assertThat(activeDday.get().getTitle()).isEqualTo("1학기 종강");
        assertThat(activeDday.get().getDDay()).isEqualTo("D-3");
    }

    @Test
    @DisplayName("오늘 날짜의 D-Day 일정이 이미 시간이 지난 경우 제외하고 다음 미래 일정을 반환한다")
    void getActiveDdayFilterPassedTime() {
        // given
        LocalDate today = LocalDate.now();
        LocalTime pastTime = LocalTime.MIN;
        LocalTime futureTime = LocalTime.MAX;

        DdaySetting d1 = DdaySetting.builder()
                .title("오늘 이미 지난 D-Day")
                .targetDate(today)
                .targetTime(pastTime)
                .build();
        ReflectionTestUtils.setField(d1, "id", 1L);

        DdaySetting d2 = DdaySetting.builder()
                .title("오늘 남은 D-Day")
                .targetDate(today)
                .targetTime(futureTime)
                .build();
        ReflectionTestUtils.setField(d2, "id", 2L);

        DdaySetting d3 = DdaySetting.builder()
                .title("미래 D-Day")
                .targetDate(today.plusDays(5))
                .build();
        ReflectionTestUtils.setField(d3, "id", 3L);

        when(ddaySettingRepository.findActiveDdays(today)).thenReturn(List.of(d1, d2, d3));

        // when
        Optional<DdaySettingResponse> activeDday = ddaySettingService.getActiveDday();

        // then
        assertThat(activeDday).isPresent();
        assertThat(activeDday.get().getTitle()).isEqualTo("오늘 남은 D-Day");
        assertThat(activeDday.get().getDDay()).isEqualTo("D-Day");
    }
}
