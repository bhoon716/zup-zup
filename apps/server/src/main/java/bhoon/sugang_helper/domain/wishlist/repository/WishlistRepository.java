package bhoon.sugang_helper.domain.wishlist.repository;

import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndCourseKey(Long userId, String courseKey);

    boolean existsByUserIdAndCourseKey(Long userId, String courseKey);

    void deleteByUserIdAndCourseKey(Long userId, String courseKey);
}
