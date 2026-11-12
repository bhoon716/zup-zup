package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackReplyRepository extends JpaRepository<FeedbackReply, Long> {

    @Query("select r.feedback.id from FeedbackReply r where r.id = :id")
    Optional<Long> findFeedbackIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FeedbackReply r where r.id = :id")
    Optional<FeedbackReply> findByIdForUpdate(@Param("id") Long id);
}
