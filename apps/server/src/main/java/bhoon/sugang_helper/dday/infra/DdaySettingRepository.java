package bhoon.sugang_helper.dday.infra;

import bhoon.sugang_helper.dday.domain.DdaySetting;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DdaySettingRepository extends JpaRepository<DdaySetting, Long> {

    @Query("SELECT d FROM DdaySetting d WHERE d.targetDate >= :today ORDER BY d.targetDate ASC, d.targetTime ASC")
    List<DdaySetting> findActiveDdays(@Param("today") LocalDate today);

    List<DdaySetting> findAllByOrderByTargetDateAscTargetTimeAsc();
}
