package bhoon.sugang_helper.domain.user.repository;

import bhoon.sugang_helper.domain.user.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    List<UserDevice> findByUserIdAndType(Long userId, bhoon.sugang_helper.domain.user.enums.DeviceType type);

    List<UserDevice> findByUserIdIn(List<Long> userIds);

    Optional<UserDevice> findByToken(String token);
}
