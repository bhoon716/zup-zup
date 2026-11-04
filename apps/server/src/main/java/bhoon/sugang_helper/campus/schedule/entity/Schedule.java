package bhoon.sugang_helper.campus.schedule.entity;

import bhoon.sugang_helper.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedules")
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String scheduleType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean ddayEnabled;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    public Schedule(String scheduleType, LocalDate startDate, LocalDate endDate, LocalTime startTime,
                    LocalTime endTime, boolean ddayEnabled) {
        this.scheduleType = scheduleType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ddayEnabled = ddayEnabled;
    }

    public void update(String scheduleType, LocalDate startDate, LocalDate endDate, LocalTime startTime,
                       LocalTime endTime, boolean ddayEnabled) {
        this.scheduleType = scheduleType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ddayEnabled = ddayEnabled;
    }

    public LocalDateTime startsAt() {
        return startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
    }

    public LocalDateTime endsAt() {
        return endTime == null ? endDate.plusDays(1).atStartOfDay() : endDate.atTime(endTime);
    }

    public boolean isOngoingAt(LocalDateTime now) {
        return !now.isBefore(startsAt()) && now.isBefore(endsAt());
    }

    public void delete(LocalDateTime now) {
        deletedAt = now;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void restore() {
        deletedAt = null;
    }
}
