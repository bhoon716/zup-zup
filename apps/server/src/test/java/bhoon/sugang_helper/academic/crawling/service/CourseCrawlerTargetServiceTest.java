package bhoon.sugang_helper.academic.crawling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.dto.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerSetting;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerSettingRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CourseCrawlerTargetServiceTest {

    private static final String ACADEMIC_YEAR = "2026";

    @Mock
    private CrawlerSettingRepository crawlerSettingRepository;

    @InjectMocks
    private CourseCrawlerTargetService crawlerTargetService;

    @Test
    @DisplayName("검색 기본 학기는 현재 크롤링 타겟 학기를 따른다")
    void getSearchDefaultSemester_ReturnsCurrentTargetSemester() {

        CrawlerSetting setting = CrawlerSetting.builder()
                .targetYear(ACADEMIC_YEAR)
                .targetSemester(SemesterType.SUMMER_SESSION.getCode())
                .build();
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.of(setting));
        ReflectionTestUtils.setField(crawlerTargetService, "defaultSemester", SemesterType.FIRST_SEMESTER.getCode());


        var response = crawlerTargetService.getSearchDefaultSemester();


        assertThat(response.getSemester()).isEqualTo(SemesterType.SUMMER_SESSION.getCode());
        verify(crawlerSettingRepository).findTopByOrderByIdAsc();
    }

    @Test
    @DisplayName("DB에 설정이 존재하지 않으면 현재 타겟은 기본 설정값을 조회만 하고 저장하지 않는다")
    void getCurrentTarget_NotFound_ReturnsDefaultValueWithoutSaving() {

        ReflectionTestUtils.setField(crawlerTargetService, "defaultYear", ACADEMIC_YEAR);
        ReflectionTestUtils.setField(crawlerTargetService, "defaultSemester", SemesterType.FIRST_SEMESTER.getCode());
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.empty());


        AdminCrawlTargetResponse response = crawlerTargetService.getCurrentTarget();


        assertThat(response.getYear()).isEqualTo(ACADEMIC_YEAR);
        assertThat(response.getSemester()).isEqualTo(SemesterType.FIRST_SEMESTER.getCode());
        verify(crawlerSettingRepository, never()).save(any(CrawlerSetting.class));
    }

    @Test
    @DisplayName("DB에 설정이 존재하지 않으면 검색 기본 학기도 기본값을 조회만 하고 저장하지 않는다")
    void getSearchDefaultSemester_NotFound_ReturnsDefaultValueWithoutSaving() {

        ReflectionTestUtils.setField(crawlerTargetService, "defaultYear", ACADEMIC_YEAR);
        ReflectionTestUtils.setField(crawlerTargetService, "defaultSemester", SemesterType.FIRST_SEMESTER.getCode());
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.empty());


        var response = crawlerTargetService.getSearchDefaultSemester();


        assertThat(response.getSemester()).isEqualTo(SemesterType.FIRST_SEMESTER.getCode());
        verify(crawlerSettingRepository, never()).save(any(CrawlerSetting.class));
    }

    @Test
    @DisplayName("DB에 설정이 존재하지 않으면 현재 타겟 원시 값도 기본값을 조회만 하고 저장하지 않는다")
    void getCurrentTargetValue_NotFound_ReturnsDefaultValueWithoutSaving() {

        ReflectionTestUtils.setField(crawlerTargetService, "defaultYear", ACADEMIC_YEAR);
        ReflectionTestUtils.setField(crawlerTargetService, "defaultSemester", SemesterType.FIRST_SEMESTER.getCode());
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.empty());


        CrawlTargetInfo targetInfo = crawlerTargetService.getCurrentTargetValue();


        assertThat(targetInfo.year()).isEqualTo(ACADEMIC_YEAR);
        assertThat(targetInfo.semester()).isEqualTo(SemesterType.FIRST_SEMESTER);
        verify(crawlerSettingRepository, never()).save(any(CrawlerSetting.class));
    }

    @Test
    @DisplayName("현재 타겟의 원시 값(Record)을 정상적으로 조회한다")
    void getCurrentTargetValue_Success() {

        CrawlerSetting setting = CrawlerSetting.builder()
                .targetYear(ACADEMIC_YEAR)
                .targetSemester(SemesterType.WINTER_SESSION.getCode())
                .build();
        given(crawlerSettingRepository.findTopByOrderByIdAsc()).willReturn(Optional.of(setting));


        CrawlTargetInfo targetInfo = crawlerTargetService.getCurrentTargetValue();


        assertThat(targetInfo.year()).isEqualTo(ACADEMIC_YEAR);
        assertThat(targetInfo.semester()).isEqualTo(SemesterType.WINTER_SESSION);
    }
}
