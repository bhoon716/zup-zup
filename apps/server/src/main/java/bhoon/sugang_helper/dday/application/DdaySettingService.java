package bhoon.sugang_helper.dday.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.dday.domain.DdaySetting;
import bhoon.sugang_helper.dday.domain.DdaySettingRepository;
import bhoon.sugang_helper.dday.presentation.DdaySettingRequest;
import bhoon.sugang_helper.dday.presentation.DdaySettingResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DdaySettingService {

    private final DdaySettingRepository ddaySettingRepository;

    /**
     * 유저용: 현재 시간 기준 가장 가까운 미래의 D-Day를 하나 반환합니다.
     */
    public Optional<DdaySettingResponse> getActiveDday() {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<DdaySetting> activeDdays = ddaySettingRepository.findActiveDdays(today);

        return activeDdays.stream()
                .filter(d -> {
                    if (d.getTargetDate().isEqual(today) && d.getTargetTime() != null) {
                        return nowTime.isBefore(d.getTargetTime());
                    }
                    return true;
                })
                .findFirst()
                .map(DdaySettingResponse::from);
    }

    /**
     * 어드민용: 모든 D-Day 설정을 최근 목표일 순으로 조회
     */
    public List<DdaySettingResponse> getAllDdays() {
        return ddaySettingRepository.findAllByOrderByTargetDateAscTargetTimeAsc()
                .stream()
                .map(DdaySettingResponse::from)
                .toList();
    }

    /**
     * 신규 D-Day 생성
     */
    @Transactional
    public DdaySettingResponse createDday(DdaySettingRequest request) {
        DdaySetting setting = DdaySetting.builder()
                .title(request.getTitle())
                .targetDate(request.getTargetDate())
                .targetTime(request.getTargetTime())
                .build();
        return DdaySettingResponse.from(ddaySettingRepository.save(setting));
    }

    /**
     * D-Day 수정
     */
    @Transactional
    public DdaySettingResponse updateDday(Long id, DdaySettingRequest request) {
        DdaySetting setting = getDdayEntity(id);
        setting.update(request.getTitle(), request.getTargetDate(), request.getTargetTime());
        return DdaySettingResponse.from(setting);
    }

    /**
     * D-Day 삭제
     */
    @Transactional
    public void deleteDday(Long id) {
        DdaySetting setting = getDdayEntity(id);
        ddaySettingRepository.delete(setting);
    }

    private DdaySetting getDdayEntity(Long id) {
        return ddaySettingRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "해당 D-Day 설정을 찾을 수 없습니다."));
    }
}
