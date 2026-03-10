package bhoon.sugang_helper.domain.feedback.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.feedback.entity.enums.ActionType;
import bhoon.sugang_helper.domain.feedback.entity.enums.TargetType;
import bhoon.sugang_helper.domain.user.entity.User;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자의 주요 액션(상태 변경, 답변 등록 등)을 기록하는 로그 엔티티 클래스입니다.
 * 운영 감사(Audit) 목적으로 사용되며, 관리자 ID와 수행한 액션, 대상 정보를 기록합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_action_logs")
public class AdminActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String metaData;

    @Builder
    public AdminActionLog(User admin, ActionType actionType, TargetType targetType, Long targetId, String metaData) {
        this.admin = admin;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metaData = metaData;
    }
}
