package bhoon.sugang_helper.domain.wishlist.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "찜 토글 결과 응답 DTO")
public class WishlistToggleResponse {
    @Schema(description = "찜 상태", example = "true")
    private boolean isWished;

    public static WishlistToggleResponse of(boolean isWished) {
        return WishlistToggleResponse.builder()
                .isWished(isWished)
                .build();
    }
}
