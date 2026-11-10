package bhoon.sugang_helper.academic.course.repository;

import bhoon.sugang_helper.academic.api.catalog.CourseName;
import bhoon.sugang_helper.academic.course.entity.Course;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseKey(String courseKey);

    @EntityGraph(attributePaths = "schedules")
    Optional<Course> findWithSchedulesByCourseKey(String courseKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.courseKey = :courseKey")
    Optional<Course> findByCourseKeyForUpdate(@Param("courseKey") String courseKey);

    boolean existsByCourseKey(String courseKey);

    List<Course> findByCourseKeyIn(List<String> courseKeys);

    @EntityGraph(attributePaths = "schedules")
    List<Course> findWithSchedulesByCourseKeyIn(List<String> courseKeys);

    @Query("""
            select new bhoon.sugang_helper.academic.api.catalog.CourseName(c.courseKey, c.name, c.professor)
            from Course c
            where c.courseKey in :courseKeys
            """)
    List<CourseName> findNamesByCourseKeyIn(@Param("courseKeys") List<String> courseKeys);

    @Query("""
            select c.courseKey
            from Course c
            where c.subjectCode = :subjectCode
              and coalesce(trim(c.professor), '') = :professor
            """)
    List<String> findCourseKeysBySubjectCodeAndProfessor(@Param("subjectCode") String subjectCode,
                                                         @Param("professor") String professor);

    @Query(value = """
            with categories as (
                select distinct general_category as category
                from courses
                where general_category is not null and general_category <> ''
            )
            select distinct categories.category as category, nullif(c.general_detail, '') as detail
            from categories
            join courses c on c.general_category = categories.category
            order by category, detail
            """, nativeQuery = true)
    List<GeneralCategoryDetail> findGeneralCategoryDetails();

    interface GeneralCategoryDetail {
        String getCategory();

        String getDetail();
    }
}
