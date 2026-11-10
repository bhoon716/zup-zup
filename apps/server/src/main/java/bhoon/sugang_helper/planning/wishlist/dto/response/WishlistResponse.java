package bhoon.sugang_helper.planning.wishlist.dto.response;

import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.planning.wishlist.entity.Wishlist;
import java.time.LocalDateTime;
import java.util.Objects;

public record WishlistResponse(
    Long id,
    String courseKey,
    String subjectCode,
    String classNumber,
    String courseName,
    String professor,
    String classification,
    String credits,
    String classTime,
    Integer current,
    Integer capacity,
    Integer available,
    LocalDateTime createdAt
) {
    public static WishlistResponse from(Wishlist wishlist, CourseSummary course) {
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getCourseKey(),
                course.subjectCode(),
                course.classNumber(),
                course.name(),
                course.professor(),
                Objects.requireNonNullElse(course.classification(), ""),
                course.credits(),
                course.classTime(),
                course.current(),
                course.capacity(),
                course.available(),
                wishlist.getCreatedAt());
    }
}
