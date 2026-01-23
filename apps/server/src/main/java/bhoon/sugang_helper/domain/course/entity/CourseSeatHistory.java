package bhoon.sugang_helper.domain.course.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_seat_histories", indexes = {
        @Index(name = "idx_seat_hist_course_key", columnList = "courseKey")
})
public class CourseSeatHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseKey;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer current;

    @Builder
    public CourseSeatHistory(String courseKey, Integer capacity, Integer current) {
        this.courseKey = courseKey;
        this.capacity = capacity;
        this.current = current;
    }
}
