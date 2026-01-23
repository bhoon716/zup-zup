package bhoon.sugang_helper.domain.course.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @Column(length = 20)
    private String courseKey; // '과목코드-분반'

    @Column(nullable = false, length = 20)
    private String subjectCode; // 과목코드

    @Column(nullable = false, length = 100)
    private String name; // 과목명

    @Column(nullable = false, length = 5)
    private String classNumber; // 분반 (e.g., "01")

    @Column(length = 50)
    private String professor; // 교수 이름

    @Column(nullable = false)
    private Integer capacity; // 정원

    @Column(nullable = false)
    private Integer current; // 신청인원

    @Column(nullable = false)
    private LocalDateTime lastCrawledAt; // 마지막 크롤링 시간

    @Builder
    public Course(String courseKey, String subjectCode, String classNumber, String name, String professor,
                  Integer capacity, Integer current) {
        this.courseKey = courseKey;
        this.subjectCode = subjectCode;
        this.classNumber = classNumber;
        this.name = name;
        this.professor = professor;
        this.capacity = capacity;
        this.current = current;
        this.lastCrawledAt = LocalDateTime.now();
    }

    public int getAvailable() {
        return Math.max(0, capacity - current);
    }

    public void updateStatus(Integer capacity, Integer current) {
        this.capacity = capacity;
        this.current = current;
        this.lastCrawledAt = LocalDateTime.now();
    }
}
