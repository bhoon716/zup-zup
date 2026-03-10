package bhoon.sugang_helper.domain.feedback.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
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

/**
 * 피드백에 첨부된 파일 정보를 관리하는 엔티티 클래스입니다.
 * 원본 파일명과 서버에 저장된 파일 URL을 기록합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedback_attachments")
public class FeedbackAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(nullable = false, length = 255)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Builder
    public FeedbackAttachment(Feedback feedback, String fileUrl, String originalName) {
        this.feedback = feedback;
        this.fileUrl = fileUrl;
        this.originalName = originalName;
    }
}
