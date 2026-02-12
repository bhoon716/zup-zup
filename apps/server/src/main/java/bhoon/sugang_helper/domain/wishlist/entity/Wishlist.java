package bhoon.sugang_helper.domain.wishlist.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wishlists", indexes = {
        @Index(name = "idx_wishlist_user_id", columnList = "userId"),
        @Index(name = "idx_wishlist_course_key", columnList = "courseKey")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishlist_user_course", columnNames = { "userId", "courseKey" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String courseKey;

    @Builder
    public Wishlist(Long userId, String courseKey) {
        this.userId = userId;
        this.courseKey = courseKey;
    }
}
