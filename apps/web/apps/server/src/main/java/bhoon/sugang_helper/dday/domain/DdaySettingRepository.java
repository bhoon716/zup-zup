package bhoon.sugang_helper.dday.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.time.LocalDate;
import java.util.List;

public interface DdaySettingRepository extends RepositoryContract<DdaySetting, Long> {
    List<DdaySetting> findAllByOrderByTargetDateAscTargetTimeAsc();

    List<DdaySetting> findActiveDdays(LocalDate today);
}
