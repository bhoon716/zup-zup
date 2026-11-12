package bhoon.sugang_helper.identity.user.repository;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    List<UserDevice> findByUserIdIn(List<Long> userIds);

    Optional<UserDevice> findByToken(String token);

    @Modifying
    @Query("delete from UserDevice d where d.token = :token and d.userId = :userId")
    int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") Long userId);

    @Modifying
    @Query("""
            delete from UserDevice d
            where d.id = :#{#device.id} and d.userId = :#{#device.userId} and d.token = :#{#device.token}
              and d.p256dh = :#{#device.p256dh} and d.auth = :#{#device.auth}
            """)
    int deleteMatchingSubscription(@Param("device") NotificationDevice device);

    @Modifying
    @Query("delete from UserDevice d where d.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
