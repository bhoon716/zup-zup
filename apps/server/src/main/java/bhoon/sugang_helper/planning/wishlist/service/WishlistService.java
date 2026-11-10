package bhoon.sugang_helper.planning.wishlist.service;

import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.api.users.Users;
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
    private final CourseCatalog courseCatalogQuery;
    private final Users userIdentityQuery;

    @Transactional
    public WishlistToggleResponse toggleWishlist(String courseKey) {
        UserIdentity user = getCurrentUser();
        validateCourseExists(courseKey);

        Wishlist existingWishlist = wishlistRepository.findByUserIdAndCourseKey(user.id(), courseKey)
                .orElse(null);
        if (existingWishlist != null) {
            wishlistRepository.delete(existingWishlist);
            logWishlistCourseRemoved(user.id(), courseKey);
            return WishlistToggleResponse.of(false);
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(user.id())
                .courseKey(courseKey)
                .build();
        wishlistRepository.save(wishlist);
        logWishlistCourseAdded(user.id(), courseKey);
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
            logWishlistCourseMissing(wishlist.getCourseKey());
            return null;
        }
        return WishlistResponse.from(wishlist, course);
    }

    private UserIdentity getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private void logWishlistCourseRemoved(Long userId, String courseKey) {
        log.atDebug().addKeyValue("event", "wishlist_course_removed")
                .addKeyValue("userId", userId)
                .addKeyValue("courseKey", courseKey)
                .log("[Wishlist] 관심 강의 제거.");
    }

    private void logWishlistCourseAdded(Long userId, String courseKey) {
        log.atDebug().addKeyValue("event", "wishlist_course_added")
                .addKeyValue("userId", userId)
                .addKeyValue("courseKey", courseKey)
                .log("[Wishlist] 관심 강의 추가.");
    }

    private void logWishlistCourseMissing(String courseKey) {
        log.atWarn().addKeyValue("event", "wishlist_course_missing")
                .addKeyValue("courseKey", courseKey)
                .log("[Wishlist] 강의 정보가 없어 응답에서 제외.");
    }

}
