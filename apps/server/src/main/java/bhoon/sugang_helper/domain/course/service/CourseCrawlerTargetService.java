package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.enums.SemesterType;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.CrawlerSetting;
import bhoon.sugang_helper.domain.course.repository.CrawlerSettingRepository;
import bhoon.sugang_helper.domain.course.response.AdminCrawlTargetResponse;
import bhoon.sugang_helper.domain.course.response.CrawlTargetInfo;
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

    /**
     * 현재 DB에 저장된 크롤링 타겟(년도, 학기)을 조회합니다.
     */
    @Transactional
    public AdminCrawlTargetResponse getCurrentTarget() {
        CrawlerSetting setting = getOrInitSetting();
        return toResponse(setting);
    }

    /**
     * 크롤링 타겟(년도, 학기)을 수정하고 저장합니다.
     */
    @Transactional
    public AdminCrawlTargetResponse updateTarget(String year, String semester) {
        CrawlTargetInfo target = normalizeTarget(year, semester);
        CrawlerSetting setting = getOrInitSetting();
        setting.updateTarget(target.year(), target.semester().getCode());
        return toResponse(setting);
    }

    /**
     * 현재 크롤링 타겟의 원시 값(record 형식)을 조회합니다.
     */
    @Transactional
    public CrawlTargetInfo getCurrentTargetValue() {
        CrawlerSetting setting = getOrInitSetting();
        return new CrawlTargetInfo(setting.getTargetYear(), SemesterType.fromCode(setting.getTargetSemester()));
    }

    /**
     * 입력된 년도와 학기 문자열을 검증하고 정규화합니다.
     */
    public CrawlTargetInfo normalizeTarget(String year, String semester) {
        String normalizedYear = normalizeYear(year);
        SemesterType normalizedSemester = normalizeSemesterType(semester);
        return new CrawlTargetInfo(normalizedYear, normalizedSemester);
    }

    /**
     * 년도 문자열이 4자리 숫자인지 검증하고 공백을 제거합니다.
     */
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

    /**
     * 학기 코드 문자열을 검증하고 SemesterType을 반환합니다.
     */
    private SemesterType normalizeSemesterType(String semester) {
        if (semester == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "학기 코드는 필수입니다.");
        }
        String normalized = semester.trim();
        try {
            return SemesterType.fromCode(normalized);
        } catch (CustomException e) {
            // 지원하는 학기 목록을 포함한 더 친절한 에러 메시지로 보강
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 학기 코드입니다. (지원대상: 1학기, 2학기, 하기/동기 계절학기, 여름/겨울/신입생/SW 특별학기)");
        }
    }

    /**
     * DB에서 크롤링 설정을 조회합니다. 없을 경우 설정 파일의 기본값으로 초기화하여 반환합니다.
     */
    private CrawlerSetting getOrInitSetting() {
        return crawlerSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    CrawlerSetting defaultSetting = CrawlerSetting.builder()
                            .targetYear(defaultYear)
                            .targetSemester(defaultSemester)
                            .build();
                    return crawlerSettingRepository.save(defaultSetting);
                });
    }

    /**
     * CrawlerSetting 엔티티를 응답 DTO로 변환합니다.
     */
    private AdminCrawlTargetResponse toResponse(CrawlerSetting setting) {
        return AdminCrawlTargetResponse.builder()
                .year(setting.getTargetYear())
                .semester(setting.getTargetSemester())
                .build();
    }
}
