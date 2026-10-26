package bhoon.sugang_helper.academic.course.entity;

import bhoon.sugang_helper.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_seat_histories")
public class CourseSeatHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
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
