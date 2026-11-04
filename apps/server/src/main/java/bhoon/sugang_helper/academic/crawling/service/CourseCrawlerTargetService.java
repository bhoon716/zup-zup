package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.enums.SemesterType;
import bhoon.sugang_helper.academic.crawling.dto.result.CrawlTargetInfo;
import bhoon.sugang_helper.academic.crawling.dto.response.CrawlerAdminSnapshot;
import bhoon.sugang_helper.academic.crawling.dto.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.academic.crawling.dto.response.SearchDefaultSemesterResponse;
import bhoon.sugang_helper.academic.crawling.entity.CrawlerSetting;
import bhoon.sugang_helper.academic.crawling.repository.CrawlerSettingRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCrawlerTargetService {

    private final CrawlerSettingRepository crawlerSettingRepository;

    @Value("${jbnu.crawler.default-year}")
    private String defaultYear;

    @Value("${jbnu.crawler.default-semester}")
    private String defaultSemester;




    @Transactional(readOnly = true)
    public AdminCrawlTargetResponse getCurrentTarget() {
        CrawlerSetting setting = resolveCurrentSetting();
        return toResponse(setting);
    }

    public CrawlerAdminSnapshot findCurrentAdminSnapshot() {
        CrawlerSetting setting = resolveCurrentSetting();
        return new CrawlerAdminSnapshot(setting.getTargetYear(), setting.getTargetSemester());
    }




    @Transactional(readOnly = true)
    public SearchDefaultSemesterResponse getSearchDefaultSemester() {
        CrawlerSetting setting = resolveCurrentSetting();
        return SearchDefaultSemesterResponse.builder()
                .semester(setting.getTargetSemester())
                .build();
    }




    @Transactional
    public AdminCrawlTargetResponse updateTarget(String year, String semester) {
        CrawlTargetInfo target = normalizeTarget(year, semester);
        CrawlerSetting setting = getOrCreateSetting();
        setting.updateTarget(target.year(), target.semester().getCode());
        return toResponse(setting);
    }




    @Transactional(readOnly = true)
    public CrawlTargetInfo getCurrentTargetValue() {
        CrawlerSetting setting = resolveCurrentSetting();
        return new CrawlTargetInfo(setting.getTargetYear(), SemesterType.fromCode(setting.getTargetSemester()));
    }




    public CrawlTargetInfo normalizeTarget(String year, String semester) {
        String normalizedYear = normalizeYear(year);
        SemesterType normalizedSemester = normalizeSemesterType(semester);
        return new CrawlTargetInfo(normalizedYear, normalizedSemester);
    }




    private String normalizeYear(String year) {
        if (year == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "년도는 필수입니다.");
        }
        String normalized = year.trim();
        if (!normalized.matches("\\d{4}")) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "년도는 4자리 숫자여야 합니다.");
        }
        return normalized;
    }




    private SemesterType normalizeSemesterType(String semester) {
        if (semester == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "학기 코드는 필수입니다.");
        }
        String normalized = semester.trim();
        try {
            return SemesterType.fromCode(normalized);
        } catch (CustomException e) {

            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 학기 코드입니다. (지원대상: 1학기, 2학기, 하기/동기 계절학기, 여름/겨울/신입생/SW 특별학기)");
        }
    }




    private CrawlerSetting resolveCurrentSetting() {
        return crawlerSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> CrawlerSetting.builder()
                        .targetYear(defaultYear)
                        .targetSemester(defaultSemester)
                        .build());
    }




    private CrawlerSetting getOrCreateSetting() {
        return crawlerSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    CrawlerSetting defaultSetting = CrawlerSetting.builder()
                            .targetYear(defaultYear)
                            .targetSemester(defaultSemester)
                            .build();
                    return crawlerSettingRepository.save(defaultSetting);
                });
    }




    private AdminCrawlTargetResponse toResponse(CrawlerSetting setting) {
        return AdminCrawlTargetResponse.builder()
                .year(setting.getTargetYear())
                .semester(setting.getTargetSemester())
                .build();
    }

}
