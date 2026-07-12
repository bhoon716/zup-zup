package bhoon.sugang_helper.feedback.infra;

import bhoon.sugang_helper.feedback.domain.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackReadRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Hibernate의 전역 soft-delete restriction 밖에서 관리자용 최소 projection을 조회합니다.
 */
@Repository
@RequiredArgsConstructor
public class JdbcAdminFeedbackReadRepository implements AdminFeedbackReadRepository {

    private static final String DELETED_AT_COLUMN = "deleted_at";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<FeedbackSummary> findFeedbacks(AdminFeedbackDeletionFilter deletionFilter, Pageable pageable) {
        String condition = deletionCondition(deletionFilter);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feedbacks f WHERE " + condition, Long.class);
        String sql = """
                SELECT f.id, f.type, f.title, f.status, f.created_at, f.deleted_at,
                       EXISTS (SELECT 1 FROM feedback_replies r WHERE r.feedback_id = f.id) AS has_replies,
                       CASE WHEN u.id IS NULL OR u.deleted_at IS NOT NULL THEN TRUE ELSE FALSE END AS author_withdrawn
                FROM feedbacks f
                LEFT JOIN users u ON u.id = f.user_id
                WHERE %s
                ORDER BY f.created_at DESC, f.id DESC
                LIMIT ? OFFSET ?
                """.formatted(condition);
        List<FeedbackSummary> content = jdbcTemplate.query(sql,
                (resultSet, rowNum) -> new FeedbackSummary(
                        resultSet.getLong("id"),
                        FeedbackType.valueOf(resultSet.getString("type")),
                        resultSet.getString("title"),
                        FeedbackStatus.valueOf(resultSet.getString("status")),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getBoolean("has_replies"),
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class),
                        resultSet.getBoolean("author_withdrawn")),
                pageable.getPageSize(), offset(pageable));
        return new PageImpl<>(content, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), total);
    }

    @Override
    public Optional<FeedbackDetail> findFeedbackDetail(Long feedbackId) {
        List<FeedbackDetail> details = jdbcTemplate.query("""
                        SELECT f.id, f.type, f.title, f.content, f.status, f.created_at, f.deleted_at,
                               CASE WHEN u.id IS NULL OR u.deleted_at IS NOT NULL THEN TRUE ELSE FALSE END AS author_withdrawn
                        FROM feedbacks f
                        LEFT JOIN users u ON u.id = f.user_id
                        WHERE f.id = ?
                        """,
                (resultSet, rowNum) -> new FeedbackDetail(
                        resultSet.getLong("id"),
                        FeedbackType.valueOf(resultSet.getString("type")),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        FeedbackStatus.valueOf(resultSet.getString("status")),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class),
                        resultSet.getBoolean("author_withdrawn")),
                feedbackId);
        return details.stream().findFirst();
    }

    @Override
    public List<FeedbackReplySummary> findReplies(Long feedbackId) {
        return jdbcTemplate.query("""
                        SELECT id, content, created_at, updated_at, deleted_at
                        FROM feedback_replies
                        WHERE feedback_id = ?
                        ORDER BY created_at ASC, id ASC
                        """,
                (resultSet, rowNum) -> new FeedbackReplySummary(
                        resultSet.getLong("id"),
                        resultSet.getString("content"),
                        resultSet.getObject("created_at", LocalDateTime.class),
                        resultSet.getObject("updated_at", LocalDateTime.class),
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                        resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class)),
                feedbackId);
    }

    @Override
    public List<FeedbackAttachmentSummary> findAttachments(Long feedbackId) {
        return jdbcTemplate.query("""
                        SELECT id
                        FROM feedback_attachments
                        WHERE feedback_id = ?
                        ORDER BY id ASC
                        """,
                (resultSet, rowNum) -> new FeedbackAttachmentSummary(resultSet.getLong("id")),
                feedbackId);
    }

    @Override
    public Optional<FeedbackAttachmentAccess> findAttachmentForAdmin(Long feedbackId, Long attachmentId) {
        List<FeedbackAttachmentAccess> attachments = jdbcTemplate.query("""
                        SELECT a.id, a.feedback_id, a.file_url, a.original_name
                        FROM feedback_attachments a
                        INNER JOIN feedbacks f ON f.id = a.feedback_id
                        WHERE a.feedback_id = ?
                          AND a.id = ?
                        """,
                (resultSet, rowNum) -> new FeedbackAttachmentAccess(
                        resultSet.getLong("id"),
                        resultSet.getLong("feedback_id"),
                        resultSet.getString("file_url"),
                        resultSet.getString("original_name")),
                feedbackId,
                attachmentId);
        return attachments.stream().findFirst();
    }

    private String deletionCondition(AdminFeedbackDeletionFilter deletionFilter) {
        return switch (deletionFilter) {
            case ALL -> "1 = 1";
            case ACTIVE -> "f.deleted_at IS NULL";
            case DELETED -> "f.deleted_at IS NOT NULL";
        };
    }

    private long offset(Pageable pageable) {
        return (long) pageable.getPageNumber() * pageable.getPageSize();
    }
}
