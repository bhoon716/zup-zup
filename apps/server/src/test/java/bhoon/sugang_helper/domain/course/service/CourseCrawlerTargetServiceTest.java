package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.domain.course.entity.CrawlerSetting;
import bhoon.sugang_helper.domain.course.enums.SemesterType;
import bhoon.sugang_helper.domain.course.repository.CrawlerSettingRepository;
import bhoon.sugang_helper.domain.course.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.domain.course.response.CrawlTargetInfo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseCrawlerTargetServiceTest {

    @Mock
    private CrawlerSettingRepository crawlerSettingRepository;

    @InjectMocks
    private CourseCrawlerTargetService crawlerTargetService;

    @Test
    @DisplayName("DB에 설정이 존재하면 정상적으로 현재 타겟을 조회한다")
    void getCurrentTarget_Success() {
        // given
        CrawlerSetting setting = CrawlerSetting.builder()
                .targetYear("2026")
                .targetSemester(SemesterType.SUMMER_SESSION.getCode())
                .build();
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.of(setting));

        // when
        AdminCrawlTargetResponse response = crawlerTargetService.getCurrentTarget();

        // then
        assertThat(response.getYear()).isEqualTo("2026");
        assertThat(response.getSemester()).isEqualTo(SemesterType.SUMMER_SESSION.getCode());
    }

    @Test
    @DisplayName("DB에 설정이 존재하지 않으면 기본 설정값으로 초기화하여 반환한다")
    void getCurrentTarget_NotFound_ReturnsDefaultValue() {
        // given
        ReflectionTestUtils.setField(crawlerTargetService, "defaultYear", "2026");
        ReflectionTestUtils.setField(crawlerTargetService, "defaultSemester", SemesterType.FIRST_SEMESTER.getCode());
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.empty());
        given(crawlerSettingRepository.save(any(CrawlerSetting.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminCrawlTargetResponse response = crawlerTargetService.getCurrentTarget();

        // then
        assertThat(response.getYear()).isEqualTo("2026");
        assertThat(response.getSemester()).isEqualTo(SemesterType.FIRST_SEMESTER.getCode());
        verify(crawlerSettingRepository, times(1)).save(any(CrawlerSetting.class));
    }

    @Test
    @DisplayName("타겟 정보를 수정하면 정상적으로 DB에 저장된다")
    void updateTarget_Success() {
        // given
        CrawlerSetting setting = CrawlerSetting.builder()
                .targetYear("2026")
                .targetSemester(SemesterType.FIRST_SEMESTER.getCode())
                .build();
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.of(setting));

        // when
        AdminCrawlTargetResponse response = crawlerTargetService.updateTarget("2026", SemesterType.SUMMER_SESSION.getCode());

        // then
        assertThat(response.getSemester()).isEqualTo(SemesterType.SUMMER_SESSION.getCode());
        assertThat(setting.getTargetSemester()).isEqualTo(SemesterType.SUMMER_SESSION.getCode());
    }

    @Test
    @DisplayName("현재 타겟의 원시 값(Record)을 정상적으로 조회한다")
    void getCurrentTargetValue_Success() {
        // given
        CrawlerSetting setting = CrawlerSetting.builder()
                .targetYear("2026")
                .targetSemester(SemesterType.WINTER_SESSION.getCode())
                .build();
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.of(setting));

        // when
        CrawlTargetInfo targetInfo = crawlerTargetService.getCurrentTargetValue();

        // then
        assertThat(targetInfo.year()).isEqualTo("2026");
        assertThat(targetInfo.semester()).isEqualTo(SemesterType.WINTER_SESSION);
    }
}
