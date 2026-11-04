package bhoon.sugang_helper.academic.course.repository;

import bhoon.sugang_helper.academic.course.entity.Course;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {




    Optional<Course> findByCourseKey(String courseKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.courseKey = :courseKey")
    Optional<Course> findByCourseKeyForUpdate(@Param("courseKey") String courseKey);




    boolean existsByCourseKey(String courseKey);




    List<Course> findByCourseKeyIn(List<String> courseKeys);

    @Query("""
            select c.courseKey
            from Course c
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
            """)
    List<String> findCourseKeysBySubjectCodeAndProfessor(@Param("subjectCode") String subjectCode,
                                                         @Param("professor") String professor);




    @Query("""
            select distinct c.generalCategory
            from Course c
            where c.generalCategory is not null
              and c.generalCategory <> ''
            order by c.generalCategory
            """)
    List<String> findDistinctGeneralCategories();




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
