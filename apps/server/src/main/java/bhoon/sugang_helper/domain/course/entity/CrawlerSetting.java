package bhoon.sugang_helper.domain.course.entity;

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

/**
 * 크롤링 대상(년도, 학기) 설정을 저장하는 엔티티입니다.
 */
@Getter
@Entity
@Table(name = "crawler_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlerSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_year", nullable = false, length = 4)
    private String targetYear;

    @Column(name = "target_semester", nullable = false, length = 20)
    private String targetSemester;

    /**
     * 새로운 크롤링 설정을 생성합니다.
     */
    @Builder
    public CrawlerSetting(String targetYear, String targetSemester) {
        this.targetYear = targetYear;
        this.targetSemester = targetSemester;
    }

    /**
     * 크롤링 대상 년도와 학기를 업데이트합니다.
     */
    public void updateTarget(String targetYear, String targetSemester) {
        this.targetYear = targetYear;
        this.targetSemester = targetSemester;
    }
}
