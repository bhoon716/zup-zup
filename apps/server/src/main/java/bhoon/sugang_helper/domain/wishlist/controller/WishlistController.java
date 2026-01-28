package bhoon.sugang_helper.domain.wishlist.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.wishlist.response.WishlistResponse;
import bhoon.sugang_helper.domain.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{courseKey}")
    public ResponseEntity<CommonResponse<Void>> toggleWishlist(@PathVariable String courseKey) {
        wishlistService.toggleWishlist(courseKey);
        return CommonResponse.ok(null, "찜 상태가 변경되었습니다.");
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<WishlistResponse>>> getMyWishlist() {
        return CommonResponse.ok(wishlistService.getMyWishlist(), "찜 목록 조회 성공");
    }
}
