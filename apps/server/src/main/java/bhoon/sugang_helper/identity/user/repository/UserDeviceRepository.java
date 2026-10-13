package bhoon.sugang_helper.identity.user.repository;

import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    List<UserDevice> findByUserIdAndType(Long userId, DeviceType type);

    List<UserDevice> findByUserIdIn(List<Long> userIds);

    Optional<UserDevice> findByToken(String token);

    long deleteByTokenAndUserId(String token, Long userId);

    void deleteAllByUserId(Long userId);
}
