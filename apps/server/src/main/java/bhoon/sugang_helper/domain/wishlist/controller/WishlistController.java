package bhoon.sugang_helper.domain.wishlist.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.wishlist.dto.response.WishlistToggleResponse;
import bhoon.sugang_helper.domain.wishlist.response.WishlistResponse;
import bhoon.sugang_helper.domain.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Wishlist", description = "찜 목록 관련 API")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "찜 토글", description = "강의를 찜 목록에 추가하거나 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토글 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "찜 목록에 추가되었습니다.",
                      "data": {
                        "isWished": true
                      }
                    }
                    """)))
    })
    @PostMapping("/{courseKey}")
    public ResponseEntity<CommonResponse<WishlistToggleResponse>> toggleWishlist(@PathVariable String courseKey) {
        WishlistToggleResponse response = wishlistService.toggleWishlist(courseKey);
        String message = response.isWished() ? "찜 목록에 추가되었습니다." : "찜 목록에서 삭제되었습니다.";
        return CommonResponse.ok(response, message);
    }

    @Operation(summary = "내 찜 목록 조회", description = "현재 사용자의 찜 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "찜 목록을 조회했습니다.",
                      "data": [
                        { "courseKey": "2026:10:CLTR01:01", "name": "기초프로그래밍" }
                      ]
                    }
                    """)))
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<WishlistResponse>>> getMyWishlist() {
        return CommonResponse.ok(wishlistService.getMyWishlist(), "찜 목록을 조회했습니다.");
    }
}
