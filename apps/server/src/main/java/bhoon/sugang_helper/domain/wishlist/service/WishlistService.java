package bhoon.sugang_helper.domain.wishlist.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.wishlist.dto.response.WishlistToggleResponse;
import bhoon.sugang_helper.domain.wishlist.response.WishlistResponse;
import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import bhoon.sugang_helper.domain.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        // Validate Course Existence
        if (courseRepository.findByCourseKey(courseKey).isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 강좌입니다.");
        }

        boolean isWished = wishlistRepository.findByUserIdAndCourseKey(user.getId(), courseKey)
                .map(wishlist -> {
                    wishlistRepository.delete(wishlist);
                    log.info("[Wishlist] Deleted: userId={}, courseKey={}", user.getId(), courseKey);
                    return false; // Removed
                })
                .orElseGet(() -> {
                    Wishlist wishlist = Wishlist.builder()
                            .userId(user.getId())
                            .courseKey(courseKey)
                            .build();
                    wishlistRepository.save(wishlist);
                    log.info("[Wishlist] Created: userId={}, courseKey={}", user.getId(), courseKey);
                    return true; // Added
                });

        return WishlistToggleResponse.of(isWished);
    }

    public List<WishlistResponse> getMyWishlist() {
        User user = getCurrentUser();
        List<Wishlist> wishlists = wishlistRepository.findByUserId(user.getId());

        if (wishlists.isEmpty()) {
            return Collections.emptyList();
        }

        // Bulk Fetch Courses to avoid N+1
        List<String> courseKeys = wishlists.stream()
                .map(Wishlist::getCourseKey)
                .toList();

        Map<String, Course> courseMap = courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));

        return wishlists.stream()
                .map(wishlist -> {
                    Course course = courseMap.get(wishlist.getCourseKey());
                    if (course == null) {
                        log.warn("[Wishlist] Course not found for key: {}", wishlist.getCourseKey());
                        return null; // Handle missing course gracefully (or filter out)
                    }
                    return WishlistResponse.of(wishlist, course);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
