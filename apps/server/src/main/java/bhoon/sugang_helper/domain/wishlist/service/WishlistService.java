package bhoon.sugang_helper.domain.wishlist.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.wishlist.dto.response.WishlistToggleResponse;
import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import bhoon.sugang_helper.domain.wishlist.repository.WishlistRepository;
import bhoon.sugang_helper.domain.wishlist.response.WishlistResponse;
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
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public WishlistToggleResponse toggleWishlist(String courseKey) {
        User user = getCurrentUser();
        validateCourseExists(courseKey);

        Wishlist existingWishlist = wishlistRepository.findByUserIdAndCourseKey(user.getId(), courseKey)
                .orElse(null);
        if (existingWishlist != null) {
            wishlistRepository.delete(existingWishlist);
            log.info("[Wishlist] Removed course from wishlist. userId={}, courseKey={}", user.getId(), courseKey);
            return WishlistToggleResponse.of(false);
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(user.getId())
                .courseKey(courseKey)
                .build();
        wishlistRepository.save(wishlist);
        log.info("[Wishlist] Added course to wishlist. userId={}, courseKey={}", user.getId(), courseKey);
        return WishlistToggleResponse.of(true);
    }

    public List<WishlistResponse> getMyWishlist() {
        User user = getCurrentUser();
        List<Wishlist> wishlists = wishlistRepository.findByUserId(user.getId());
        if (wishlists.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Course> courseMap = findCoursesByWishlists(wishlists);
        return wishlists.stream()
                .map(wishlist -> toWishlistResponse(wishlist, courseMap))
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private void validateCourseExists(String courseKey) {
        if (courseRepository.findByCourseKey(courseKey).isPresent()) {
            return;
        }

        throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 강좌입니다.");
    }

    private Map<String, Course> findCoursesByWishlists(List<Wishlist> wishlists) {
        List<String> courseKeys = wishlists.stream()
                .map(Wishlist::getCourseKey)
                .toList();

        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));
    }

    private WishlistResponse toWishlistResponse(Wishlist wishlist, Map<String, Course> courseMap) {
        Course course = courseMap.get(wishlist.getCourseKey());
        if (course == null) {
            log.warn("[Wishlist] Course information not found, excluded from response. courseKey={}",
                    wishlist.getCourseKey());
            return null;
        }
        return WishlistResponse.of(wishlist, course);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
