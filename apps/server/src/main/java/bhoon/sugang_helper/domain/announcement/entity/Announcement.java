package bhoon.sugang_helper.domain.announcement.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
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

    @Builder
    public Announcement(String title, String content, boolean pinned, boolean published) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
    }

    /**
     * 공지사항의 제목, 내용, 고정 여부, 공개 여부를 업데이트합니다.
     */
    public void update(String title, String content, boolean pinned, boolean published) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
    }
}
