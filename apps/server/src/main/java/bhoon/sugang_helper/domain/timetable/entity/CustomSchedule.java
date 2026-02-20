package bhoon.sugang_helper.domain.timetable.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

    @Column(length = 50)
    private String professor;

    @OneToMany(mappedBy = "customSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomScheduleTime> times = new ArrayList<>();

    @Builder
    public CustomSchedule(Timetable timetable, String title, String professor) {
        this.timetable = timetable;
        this.title = title;
        this.professor = professor;
    }

    public void update(String title, String professor) {
        this.title = title;
        this.professor = professor;
    }

    public void addTime(CustomScheduleTime time) {
        this.times.add(time);
        time.setCustomSchedule(this);
    }

    public void clearTimes() {
        this.times.clear();
    }
}
