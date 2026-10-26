package bhoon.sugang_helper.planning.wishlist.service;

import bhoon.sugang_helper.academic.course.dto.CourseSummary;
import bhoon.sugang_helper.academic.course.service.CourseCatalogQueryService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQuery;
import bhoon.sugang_helper.planning.wishlist.dto.response.WishlistResponse;
import bhoon.sugang_helper.planning.wishlist.dto.response.WishlistToggleResponse;
import bhoon.sugang_helper.planning.wishlist.entity.Wishlist;
import bhoon.sugang_helper.planning.wishlist.repository.WishlistRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CourseCatalogQueryService courseCatalogQuery;
    private final UserIdentityQuery userIdentityQuery;

    @Transactional
    public WishlistToggleResponse toggleWishlist(String courseKey) {
        UserIdentity user = getCurrentUser();
        validateCourseExists(courseKey);

        Wishlist existingWishlist = wishlistRepository.findByUserIdAndCourseKey(user.id(), courseKey)
                .orElse(null);
        if (existingWishlist != null) {
            wishlistRepository.delete(existingWishlist);
            log.info("[Wishlist] Removed course from wishlist. userId={}, courseKey={}", user.id(), courseKey);
            return WishlistToggleResponse.of(false);
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(user.id())
                .courseKey(courseKey)
                .build();
        wishlistRepository.save(wishlist);
        log.info("[Wishlist] Added course to wishlist. userId={}, courseKey={}", user.id(), courseKey);
        return WishlistToggleResponse.of(true);
    }

    public List<WishlistResponse> getMyWishlist() {
        UserIdentity user = getCurrentUser();
        List<Wishlist> wishlists = wishlistRepository.findByUserId(user.id());
        if (wishlists.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, CourseSummary> courseMap = findCoursesByWishlists(wishlists);
        return wishlists.stream()
                .map(wishlist -> toWishlistResponse(wishlist, courseMap))
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private void validateCourseExists(String courseKey) {
        if (courseCatalogQuery.findByCourseKey(courseKey).isPresent()) {
            return;
        }

        throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 강좌입니다.");
    }

    private Map<String, CourseSummary> findCoursesByWishlists(List<Wishlist> wishlists) {
        List<String> courseKeys = wishlists.stream()
                .map(Wishlist::getCourseKey)
                .toList();

        return courseCatalogQuery.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(CourseSummary::courseKey, Function.identity(),
                        (existing, replacement) -> existing));
    }

    private WishlistResponse toWishlistResponse(Wishlist wishlist, Map<String, CourseSummary> courseMap) {
        CourseSummary course = courseMap.get(wishlist.getCourseKey());
        if (course == null) {
            log.warn("[Wishlist] Course information not found, excluded from response. courseKey={}",
                    wishlist.getCourseKey());
            return null;
        }
        return WishlistResponse.of(wishlist, course);
    }

    private UserIdentity getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
