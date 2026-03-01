package bhoon.sugang_helper.domain.course.repository;

import bhoon.sugang_helper.domain.course.entity.Course;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {

    /**
     * 고유 키를 기반으로 강의 정보를 조회합니다.
     */
    Optional<Course> findByCourseKey(String courseKey);

    /**
     * 특정 고유 키를 가진 강의가 존재하는지 확인합니다.
     */
    boolean existsByCourseKey(String courseKey);

    /**
     * 여러 고유 키 목록에 해당하는 강의 목록을 조회합니다.
     */
    List<Course> findByCourseKeyIn(List<String> courseKeys);

    /**
     * 전체 강의 중 가장 최근에 크롤링된 시각을 조회합니다.
     */
    @Query("select max(c.lastCrawledAt) from Course c")
    Optional<LocalDateTime> findMaxLastCrawledAt();

    /**
     * 교양 영역(카테고리) 목록을 조회합니다.
     */
    @Query("""
            select distinct c.generalCategory
            from Course c
            where c.generalCategory is not null
              and c.generalCategory <> ''
            order by c.generalCategory
            """)
    List<String> findDistinctGeneralCategories();

    /**
     * 교양 상세 영역 목록을 조회합니다.
     */
    @Query("""
            select distinct c.generalDetail
            from Course c
            where c.generalCategory = :category
              and c.generalDetail is not null
              and c.generalDetail <> ''
            order by c.generalDetail
            """)
    List<String> findDistinctGeneralDetailsByCategory(@Param("category") String category);
}
