package bhoon.sugang_helper.domain.schedule.entity;

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
@Table(name = "schedules")
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column
    private LocalTime scheduleTime;

    /**
     * 일정 엔티티를 생성하는 빌더 메서드입니다.
     */
    @Builder
    public Schedule(String title, LocalDate scheduleDate, LocalTime scheduleTime) {
        this.title = title;
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
    }

    /**
     * 일정의 정보를 수정합니다.
     */
    public void update(String title, LocalDate scheduleDate, LocalTime scheduleTime) {
        this.title = title;
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
    }
}
