package bhoon.sugang_helper.domain.dday.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "dday_settings")
public class DdaySetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column
    private LocalTime targetTime;

    @Builder
    public DdaySetting(String title, LocalDate targetDate, LocalTime targetTime) {
        this.title = title;
        this.targetDate = targetDate;
        this.targetTime = targetTime;
    }

    public void update(String title, LocalDate targetDate, LocalTime targetTime) {
        this.title = title;
        this.targetDate = targetDate;
        this.targetTime = targetTime;
    }
}
