package bhoon.sugang_helper.domain.feedback.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackType;
import bhoon.sugang_helper.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 피드백(건의사항/버그리포트)의 기본 정보를 관리하는 엔티티 클래스입니다.
 * 작성자 정보, 피드백 유형, 내용 및 처리 상태를 포함합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedbacks")
@SQLRestriction("deleted_at IS NULL")
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String metaInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedbackAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedbackReply> replies = new ArrayList<>();

    @Builder
    public Feedback(User user, FeedbackType type, String title, String content, String metaInfo) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.metaInfo = metaInfo;
        this.status = FeedbackStatus.PENDING;
    }

    /**
     * 상태 변경
     */
    public void updateStatus(FeedbackStatus status) {
        this.status = status;
    }

    /**
     * 소프트 삭제 처리
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
