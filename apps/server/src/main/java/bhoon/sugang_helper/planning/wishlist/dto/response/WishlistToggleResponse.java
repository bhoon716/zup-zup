package bhoon.sugang_helper.planning.wishlist.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WishlistToggleResponse {
    private final boolean isWished;

    public static WishlistToggleResponse of(boolean isWished) {
        return new WishlistToggleResponse(isWished);
    }
}
