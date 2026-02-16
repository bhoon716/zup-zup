package bhoon.sugang_helper.domain.wishlist.repository;

import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndCourseKey(Long userId, String courseKey);
}
