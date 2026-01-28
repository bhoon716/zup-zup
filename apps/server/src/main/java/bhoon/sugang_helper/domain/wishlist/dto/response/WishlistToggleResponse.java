package bhoon.sugang_helper.domain.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WishlistToggleResponse {
    private boolean isWished;

    public static WishlistToggleResponse of(boolean isWished) {
        return WishlistToggleResponse.builder()
                .isWished(isWished)
                .build();
    }
}
