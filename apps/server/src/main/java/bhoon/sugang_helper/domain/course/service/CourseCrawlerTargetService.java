package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.course.entity.CrawlerSetting;
import bhoon.sugang_helper.domain.course.repository.CrawlerSettingRepository;
import bhoon.sugang_helper.domain.course.response.AdminCrawlTargetResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCrawlerTargetService {

    private static final Set<String> SUPPORTED_SEMESTER_CODES = Set.of(
            "U211600010", // 1학기
            "U211600020", // 2학기
            "U211600015", // 하기 계절학기
            "U211600025", // 동기 계절학기
            "U211600016", // 여름 특별학기
            "U211600026", // 겨울 특별학기
            "U211600009", // 신입생 특별학기
            "U211600008" // SW 특별학기
    );

    private final CrawlerSettingRepository crawlerSettingRepository;

    @Value("${jbnu.crawler.default-year}")
    private String defaultYear;

    @Value("${jbnu.crawler.default-semester}")
    private String defaultSemester;

    /**
     * 현재 DB에 저장된 크롤링 타겟(년도, 학기)을 조회합니다.
     */
    public AdminCrawlTargetResponse getCurrentTarget() {
        CrawlerSetting setting = getOrCreateSetting();
        return toResponse(setting);
    }

    /**
     * 크롤링 타겟(년도, 학기)을 수정하고 저장합니다.
     */
    @Transactional
    public AdminCrawlTargetResponse updateTarget(String year, String semester) {
        CrawlTarget target = normalizeTarget(year, semester);
        CrawlerSetting setting = getOrCreateSetting();
        setting.updateTarget(target.year(), target.semester());
        return toResponse(setting);
    }

    /**
     * 현재 크롤링 타겟의 원시 값(record 형식)을 조회합니다.
     */
    public CrawlTarget getCurrentTargetValue() {
        CrawlerSetting setting = getOrCreateSetting();
        return new CrawlTarget(setting.getTargetYear(), setting.getTargetSemester());
    }

    /**
     * 입력된 년도와 학기 문자열을 검증하고 정규화합니다.
     */
    public CrawlTarget normalizeTarget(String year, String semester) {
        String normalizedYear = normalizeYear(year);
        String normalizedSemester = normalizeSemester(semester);
        return new CrawlTarget(normalizedYear, normalizedSemester);
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
     * 학기 코드 문자열을 검증하고 공백을 제거합니다.
     */
    private String normalizeSemester(String semester) {
        if (semester == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "학기 코드는 필수입니다.");
        }
        String normalized = semester.trim();
        if (!SUPPORTED_SEMESTER_CODES.contains(normalized)) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 학기 코드입니다. (1학기, 2학기, 하기/동기 계절학기, 여름/겨울/신입생/SW 특별학기)");
        }
        return normalized;
    }

    /**
     * DB에서 크롤링 설정을 조회하거나, 없을 경우 기본값으로 생성합니다.
     */
    @Transactional
    protected CrawlerSetting getOrCreateSetting() {
        return crawlerSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> crawlerSettingRepository.save(CrawlerSetting.builder()
                        .targetYear(normalizeYear(defaultYear))
                        .targetSemester(normalizeSemester(defaultSemester))
                        .build()));
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

    /**
     * 크롤링 타겟 정보를 담는 레코드입니다.
     */
    public record CrawlTarget(String year, String semester) {
    }
}
