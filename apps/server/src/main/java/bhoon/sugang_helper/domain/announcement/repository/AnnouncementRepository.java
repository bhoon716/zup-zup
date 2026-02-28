package bhoon.sugang_helper.domain.announcement.repository;

import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

  /**
   * 고정 및 최신순으로 전체 공지사항을 조회합니다.
   */
  List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();

  /**
   * 공개된 공지사항을 고정 및 최신순으로 조회합니다.
   */
  List<Announcement> findByPublishedTrueOrderByPinnedDescCreatedAtDesc();

  /**
   * 공개된 특정 공지사항을 ID로 조회합니다.
   */
  Optional<Announcement> findByIdAndPublishedTrue(Long id);

  /**
   * 제목에 키워드가 포함된 공개 공지사항을 검색합니다.
   */
  @Query("""
      SELECT a FROM Announcement a
      WHERE a.published = true
        AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
      ORDER BY a.pinned DESC, a.createdAt DESC
      """)
  List<Announcement> searchPublishedByTitle(@Param("keyword") String keyword);

  /**
   * 내용에 키워드가 포함된 공개 공지사항을 검색합니다.
   */
  @Query("""
      SELECT a FROM Announcement a
      WHERE a.published = true
        AND LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
      ORDER BY a.pinned DESC, a.createdAt DESC
      """)
  List<Announcement> searchPublishedByContent(@Param("keyword") String keyword);

  /**
   * 제목 혹은 내용에 키워드가 포함된 공개 공지사항을 검색합니다.
   */
  @Query("""
      SELECT a FROM Announcement a
      WHERE a.published = true
        AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY a.pinned DESC, a.createdAt DESC
      """)
  List<Announcement> searchPublishedByTitleOrContent(@Param("keyword") String keyword);
}
