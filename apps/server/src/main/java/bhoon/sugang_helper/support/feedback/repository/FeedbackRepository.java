package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.dto.result.FeedbackListItem;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    @Query(value = """
            select new bhoon.sugang_helper.support.feedback.dto.result.FeedbackListItem(
                f.id, f.type, f.title, f.status, f.createdAt,
                case when exists (select 1 from FeedbackReply r where r.feedback = f) then true else false end)
            from Feedback f
            where f.userId = :userId
            """, countQuery = "select count(f) from Feedback f where f.userId = :userId")
    Page<FeedbackListItem> findListItemsByUserId(@Param("userId") Long userId, Pageable pageable);

    List<Feedback> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
