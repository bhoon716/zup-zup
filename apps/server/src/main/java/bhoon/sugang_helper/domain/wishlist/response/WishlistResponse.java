package bhoon.sugang_helper.domain.wishlist.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WishlistResponse {

    private final Long id;
    private final String courseKey;
    private final String subjectCode;
    private final String classNumber;
    private final String courseName;
    private final String professor;
    private final String classification;
    private final String credits;
    private final String classTime;
    private final Integer current;
    private final Integer capacity;
    private final Integer available;
    private final LocalDateTime createdAt;

    @Builder
    public WishlistResponse(Long id, String courseKey, String subjectCode, String classNumber,
            String courseName, String professor, String classification, String credits,
            String classTime, Integer current, Integer capacity, Integer available,
            LocalDateTime createdAt) {
        this.id = id;
        this.courseKey = courseKey;
        this.subjectCode = subjectCode;
        this.classNumber = classNumber;
        this.courseName = courseName;
        this.professor = professor;
        this.classification = classification;
        this.credits = credits;
        this.classTime = classTime;
        this.current = current;
        this.capacity = capacity;
        this.available = available;
        this.createdAt = createdAt;
    }

    public static WishlistResponse of(Wishlist wishlist, Course course) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .courseKey(wishlist.getCourseKey())
                .subjectCode(course.getSubjectCode())
                .classNumber(course.getClassNumber())
                .courseName(course.getName())
                .professor(course.getProfessor())
                .classification(course.getClassification() != null ? course.getClassification().getDescription() : "")
                .credits(course.getCredits())
                .classTime(course.getClassTime())
                .current(course.getCurrent())
                .capacity(course.getCapacity())
                .available(Math.max(0, course.getCapacity() - course.getCurrent()))
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
