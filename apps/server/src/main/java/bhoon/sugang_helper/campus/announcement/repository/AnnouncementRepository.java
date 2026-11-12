package bhoon.sugang_helper.campus.announcement.repository;

import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findAllByOrderByPinnedDescCreatedAtDescIdDesc(Pageable pageable);

    Page<Announcement> findByPublishedTrueAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    Optional<Announcement> findByIdAndPublishedTrueAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Announcement a WHERE a.id = :id")
    Optional<Announcement> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND a.deletedAt IS NULL
              AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND a.deletedAt IS NULL
              AND LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByContent(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = true
              AND a.deletedAt IS NULL
              AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Announcement> searchPublishedByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);
}
