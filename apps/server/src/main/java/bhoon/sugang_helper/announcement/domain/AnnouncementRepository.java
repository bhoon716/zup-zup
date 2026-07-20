package bhoon.sugang_helper.announcement.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementRepository extends RepositoryContract<Announcement, Long> {

    /**
     * 고정 및 최신순으로 전체 공지사항을 조회합니다.
     */
    List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();

    Page<Announcement> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    /**
     * 공개된 공지사항을 고정 및 최신순으로 조회합니다.
     */
    List<Announcement> findByPublishedTrueOrderByPinnedDescCreatedAtDesc();

    Page<Announcement> findByPublishedTrueOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    /**
     * 공개된 특정 공지사항을 ID로 조회합니다.
     */
    Optional<Announcement> findByIdAndPublishedTrue(Long id);

    /**
     * 제목에 키워드가 포함된 공개 공지사항을 검색합니다.
     */
    List<Announcement> searchPublishedByTitle(String keyword);

    Page<Announcement> searchPublishedByTitle(String keyword, Pageable pageable);

    /**
     * 내용에 키워드가 포함된 공개 공지사항을 검색합니다.
     */
    List<Announcement> searchPublishedByContent(String keyword);

    Page<Announcement> searchPublishedByContent(String keyword, Pageable pageable);

    /**
     * 제목 혹은 내용에 키워드가 포함된 공개 공지사항을 검색합니다.
     */
    List<Announcement> searchPublishedByTitleOrContent(String keyword);

    Page<Announcement> searchPublishedByTitleOrContent(String keyword, Pageable pageable);
}
