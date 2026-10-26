package bhoon.sugang_helper.academic.crawling.entity;

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




    @Builder
    public CrawlerSetting(String targetYear, String targetSemester) {
        this.targetYear = targetYear;
        this.targetSemester = targetSemester;
    }




    public void updateTarget(String targetYear, String targetSemester) {
        this.targetYear = targetYear;
        this.targetSemester = targetSemester;
    }
}
