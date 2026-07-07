package bhoon.sugang_helper.wishlist.infra;

import bhoon.sugang_helper.wishlist.domain.Wishlist;
import bhoon.sugang_helper.wishlist.domain.WishlistRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistJpaRepository extends JpaRepository<Wishlist, Long>, WishlistRepository {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndCourseKey(Long userId, String courseKey);
}
