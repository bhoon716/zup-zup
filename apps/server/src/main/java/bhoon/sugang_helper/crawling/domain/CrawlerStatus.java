package bhoon.sugang_helper.crawling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crawler_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlerStatus {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_status", nullable = false, length = 20)
    private CrawlerRunStatus latestStatus;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_failure_stage", length = 30)
    private CrawlerFailureStage lastFailureStage;

    @Column(name = "last_failure_type", length = 100)
    private String lastFailureType;

    @Column(name = "last_failure_message", length = 500)
    private String lastFailureMessage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CrawlerStatus initial() {
        CrawlerStatus status = new CrawlerStatus();
        status.id = SINGLETON_ID;
        return status;
    }

    public void markSuccess(LocalDateTime occurredAt) {
        latestStatus = CrawlerRunStatus.SUCCESS;
        lastSuccessAt = occurredAt;
        updatedAt = occurredAt;
    }

    public void markFailure(LocalDateTime occurredAt, CrawlerFailureStage stage, String failureType,
                            String failureMessage) {
        latestStatus = CrawlerRunStatus.FAILURE;
        lastFailureAt = occurredAt;
        lastFailureStage = stage;
        lastFailureType = failureType;
        lastFailureMessage = failureMessage;
        updatedAt = occurredAt;
    }
}
