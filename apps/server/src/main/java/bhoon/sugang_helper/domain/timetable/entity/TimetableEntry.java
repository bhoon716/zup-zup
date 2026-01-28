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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "timetable_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_timetable_course", columnNames = { "timetable_id", "courseKey" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    @Column(nullable = false, length = 64)
    private String courseKey;

    @Builder
    public TimetableEntry(Timetable timetable, String courseKey) {
        this.timetable = timetable;
        this.courseKey = courseKey;
    }
}
