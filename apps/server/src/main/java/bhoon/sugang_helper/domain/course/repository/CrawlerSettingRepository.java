package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.CrawlerSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerSettingRepository extends JpaRepository<CrawlerSetting, Long> {

    Optional<CrawlerSetting> findTopByOrderByIdAsc();
}
