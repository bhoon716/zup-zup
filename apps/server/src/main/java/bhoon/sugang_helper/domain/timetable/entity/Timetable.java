package bhoon.sugang_helper.domain.timetable.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetables", indexes = {
        @Index(name = "idx_timetable_user_id", columnList = "userId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Timetable extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean isPrimary;

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimetableEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomSchedule> customSchedules = new ArrayList<>();

    @Builder
    public Timetable(Long userId, String name, boolean isPrimary) {
        this.userId = userId;
        this.name = name;
        this.isPrimary = isPrimary;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }

    public void addEntry(TimetableEntry entry) {
        this.entries.add(entry);
        entry.setTimetable(this);
    }

    public void removeEntry(TimetableEntry entry) {
        this.entries.remove(entry);
    }

    public void addCustomSchedule(CustomSchedule customSchedule) {
        this.customSchedules.add(customSchedule);
        customSchedule.setTimetable(this);
    }

    public void removeCustomSchedule(CustomSchedule customSchedule) {
        this.customSchedules.remove(customSchedule);
    }
}
