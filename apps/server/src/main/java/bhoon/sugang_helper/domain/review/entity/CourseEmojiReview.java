package bhoon.sugang_helper.domain.review.entity;

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
@Table(name = "course_emoji_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_user_emoji", columnNames = {"course_key", "user_id", "emoji"})
})
@EntityListeners(AuditingEntityListener.class)
public class CourseEmojiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_key", nullable = false, length = 64)
    private String courseKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String emoji;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CourseEmojiReview(String courseKey, Long userId, String emoji) {
        this.courseKey = courseKey;
        this.userId = userId;
        this.emoji = emoji;
    }
}
