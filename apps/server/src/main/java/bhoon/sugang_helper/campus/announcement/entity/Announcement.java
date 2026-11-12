package bhoon.sugang_helper.campus.announcement.entity;

import bhoon.sugang_helper.common.api.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "announcements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "first_published_at")
    private LocalDateTime firstPublishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Announcement(String title, String content, boolean pinned, boolean published) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
    }

    public void update(String title, String content, boolean pinned, boolean published) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
    }

    public boolean isPubliclyVisible() {
        return published && deletedAt == null;
    }

    public boolean recordFirstPublication(LocalDateTime now) {
        if (!isPubliclyVisible() || firstPublishedAt != null) {
            return false;
        }
        firstPublishedAt = now;
        return true;
    }

    public void delete(LocalDateTime now) {
        deletedAt = now;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void restore() {
        deletedAt = null;
    }
}
