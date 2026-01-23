package bhoon.sugang_helper.domain.user.repository;

import bhoon.sugang_helper.domain.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    Optional<UserDevice> findByToken(String token);

    void deleteByUserId(Long userId);
}
