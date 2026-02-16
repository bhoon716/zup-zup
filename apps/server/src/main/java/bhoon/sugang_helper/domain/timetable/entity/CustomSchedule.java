package bhoon.sugang_helper.domain.timetable.entity;

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
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "custom_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10)
    private String dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 20)
    private String color;

    @Builder
    public CustomSchedule(Timetable timetable, String title, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                          String color) {
        this.timetable = timetable;
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }

    public void update(String title, String dayOfWeek, LocalTime startTime, LocalTime endTime, String color) {
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }
}
