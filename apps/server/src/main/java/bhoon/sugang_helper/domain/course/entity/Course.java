package bhoon.sugang_helper.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @Column(length = 20)
    private String courseKey; // SBJTCD-CLSS (e.g., 0000100017-1)

    @Column(nullable = false)
    private String name; // SBJTNM

    private String professor; // RPSTPROFNM

    @Column(nullable = false)
    private Integer capacity; // LMTRCNT (Limit Count)

    @Column(nullable = false)
    private Integer current; // TLSNRCNT (Total Listener Count)

    @Column(nullable = false)
    private Integer available; // capacity - current

    @LastModifiedDate
    private LocalDateTime lastUpdated;

    @Builder
    public Course(String courseKey, String name, String professor, Integer capacity, Integer current) {
        this.courseKey = courseKey;
        this.name = name;
        this.professor = professor;
        this.capacity = capacity;
        this.current = current;
        this.available = Math.max(0, capacity - current);
    }

    public void updateStatus(Integer capacity, Integer current) {
        this.capacity = capacity;
        this.current = current;
        this.available = Math.max(0, capacity - current);
    }
}
