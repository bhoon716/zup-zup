package bhoon.sugang_helper.user.infra;

import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.domain.DeviceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    List<UserDevice> findByUserIdAndType(Long userId, DeviceType type);

    List<UserDevice> findByUserIdIn(List<Long> userIds);

    Optional<UserDevice> findByToken(String token);
}
