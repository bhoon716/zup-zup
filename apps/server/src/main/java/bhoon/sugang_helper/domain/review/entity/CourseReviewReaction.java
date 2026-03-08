package bhoon.sugang_helper.domain.review.entity;

import bhoon.sugang_helper.domain.review.enums.ReactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_review_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_eval_user", columnNames = { "review_id", "user_id" })
})
@EntityListeners(AuditingEntityListener.class)
public class CourseReviewReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType reactionType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CourseReviewReaction(CourseReview review, Long userId, ReactionType reactionType) {
        this.review = review;
        this.userId = userId;
        this.reactionType = reactionType;
    }

    public void updateReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
}
