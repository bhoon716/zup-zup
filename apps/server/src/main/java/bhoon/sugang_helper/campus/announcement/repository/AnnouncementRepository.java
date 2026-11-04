package bhoon.sugang_helper.campus.announcement.repository;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {




    List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();

    Page<Announcement> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);




    List<Announcement> findByPublishedTrueOrderByPinnedDescCreatedAtDesc();

    Page<Announcement> findByPublishedTrueOrderByPinnedDescCreatedAtDesc(Pageable pageable);




    Optional<Announcement> findByIdAndPublishedTrue(Long id);




    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    List<Announcement> searchPublishedByTitle(@Param("keyword") String keyword);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByTitle(@Param("keyword") String keyword, Pageable pageable);




    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    List<Announcement> searchPublishedByContent(@Param("keyword") String keyword);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByContent(@Param("keyword") String keyword, Pageable pageable);




    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    List<Announcement> searchPublishedByTitleOrContent(@Param("keyword") String keyword);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);
}
