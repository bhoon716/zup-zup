package bhoon.sugang_helper.domain.feedback.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

/**
 * 사용자의 피드백에 대해 관리자가 남긴 답변을 관리하는 엔티티 클래스입니다.
 * 답변 내용과 작성자(관리자) 정보를 포함합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedback_replies")
@SQLRestriction("deleted_at IS NULL")
public class FeedbackReply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public FeedbackReply(Feedback feedback, User admin, String content) {
        this.feedback = feedback;
        this.admin = admin;
        this.content = content;
    }

    /**
     * 답변 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }

    /**
     * 소프트 삭제 처리
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
