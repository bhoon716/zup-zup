package bhoon.sugang_helper.planning.wishlist.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WishlistToggleResponse {
    private final boolean isWished;

    public static WishlistToggleResponse added() {
        return new WishlistToggleResponse(true);
    }

    public static WishlistToggleResponse removed() {
        return new WishlistToggleResponse(false);
    }
}
