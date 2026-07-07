package bhoon.sugang_helper.wishlist.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends RepositoryContract<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndCourseKey(Long userId, String courseKey);

    void deleteAllByUserId(Long userId);
}
