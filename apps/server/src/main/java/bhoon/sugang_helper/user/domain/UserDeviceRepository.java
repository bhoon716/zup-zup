package bhoon.sugang_helper.user.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends RepositoryContract<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    List<UserDevice> findByUserIdAndType(Long userId, DeviceType type);

    List<UserDevice> findByUserIdIn(List<Long> userIds);

    Optional<UserDevice> findByToken(String token);
}
