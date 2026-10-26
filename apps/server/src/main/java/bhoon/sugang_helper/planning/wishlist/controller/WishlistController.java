package bhoon.sugang_helper.planning.wishlist.controller;

import bhoon.sugang_helper.planning.wishlist.dto.response.WishlistResponse;
import bhoon.sugang_helper.planning.wishlist.dto.response.WishlistToggleResponse;
import bhoon.sugang_helper.planning.wishlist.service.WishlistService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{courseKey}")
    public ResponseEntity<WishlistToggleResponse> toggleWishlist(@PathVariable String courseKey) {
        WishlistToggleResponse response = wishlistService.toggleWishlist(courseKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getMyWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }
}
