package bhoon.sugang_helper.domain.review.entity;

import bhoon.sugang_helper.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_reviews")
public class CourseReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String courseKey;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int dislikeCount;

    @Builder
    public CourseReview(String courseKey, Long userId, int rating, String content) {
        this.courseKey = courseKey;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0)
            this.likeCount--;
    }

    public void increaseDislikeCount() {
        this.dislikeCount++;
    }

    public void decreaseDislikeCount() {
        if (this.dislikeCount > 0)
            this.dislikeCount--;
    }
}
