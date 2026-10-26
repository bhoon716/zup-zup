package bhoon.sugang_helper.planning.wishlist.dto.response;

import bhoon.sugang_helper.academic.course.dto.result.CourseSummary;
import bhoon.sugang_helper.planning.wishlist.entity.Wishlist;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
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
    public static WishlistResponse of(Wishlist wishlist, CourseSummary course) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .courseKey(wishlist.getCourseKey())
                .subjectCode(course.subjectCode())
                .classNumber(course.classNumber())
                .courseName(course.name())
                .professor(course.professor())
                .classification(course.classification() != null ? course.classification() : "")
                .credits(course.credits())
                .classTime(course.classTime())
                .current(course.current())
                .capacity(course.capacity())
                .available(course.available())
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
