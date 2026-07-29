package bhoon.sugang_helper.crawling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crawler_run_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlerRunFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_stage", nullable = false, length = 30)
    private CrawlerFailureStage failureStage;

    @Column(name = "failure_type", nullable = false, length = 100)
    private String failureType;

    @Column(name = "failure_message", nullable = false, length = 500)
    private String failureMessage;

    public CrawlerRunFailure(LocalDateTime failedAt, CrawlerFailureStage failureStage, String failureType,
                             String failureMessage) {
        this.failedAt = failedAt;
        this.failureStage = failureStage;
        this.failureType = failureType;
        this.failureMessage = failureMessage;
    }
}
