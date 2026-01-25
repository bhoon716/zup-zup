package bhoon.sugang_helper.domain.course.entity;

import bhoon.sugang_helper.domain.course.enums.ClassPeriod;
import bhoon.sugang_helper.domain.course.enums.CourseDayOfWeek;
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

@Entity
@Table(name = "course_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_key")
    private Course course;

    @Enumerated(EnumType.STRING)
    private CourseDayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    private ClassPeriod period;

    @Builder
    public CourseSchedule(Course course, CourseDayOfWeek dayOfWeek, ClassPeriod period) {
        this.course = course;
        this.dayOfWeek = dayOfWeek;
        this.period = period;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
