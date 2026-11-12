package bhoon.sugang_helper.support.feedback.repository;

import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackAttachmentAccess;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackAttachmentSummary;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackDetail;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackReplySummary;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackSummary;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackOrder;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import java.sql.ResultSet;
import java.sql.SQLException;
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

@Repository
@RequiredArgsConstructor
public class AdminFeedbackReadRepository {

    private static final String AUTHOR_ID_COLUMN = "author_id";
    private static final String CONTENT_COLUMN = "content";
    private static final String CREATED_AT_COLUMN = "created_at";
    private static final String HAS_REPLIES_COLUMN = "has_replies";
    private static final String ID_COLUMN = "id";
    private static final String UPDATED_AT_COLUMN = "updated_at";
    private static final String FEEDBACK_ID_COLUMN = "feedback_id";
    private static final String FILE_URL_COLUMN = "file_url";
    private static final String ORIGINAL_NAME_COLUMN = "original_name";
    private static final String STATUS_COLUMN = "status";
    private static final String TITLE_COLUMN = "title";
    private static final String TYPE_COLUMN = "type";

    private static final String DELETED_AT_COLUMN = "deleted_at";

    private final JdbcTemplate jdbcTemplate;

    public Page<AdminFeedbackSummary> findFeedbacks(
            AdminFeedbackDeletionFilter deletionFilter, AdminFeedbackOrder order, Pageable pageable) {
        String condition = deletionCondition(deletionFilter);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feedbacks f WHERE " + condition, Long.class);
        String sql = """
                SELECT f.id, f.user_id AS author_id, f.type, f.title, f.status, f.created_at, f.deleted_at,
                       EXISTS (SELECT 1 FROM feedback_replies r WHERE r.feedback_id = f.id) AS has_replies
                FROM feedbacks f
                WHERE %s
                ORDER BY %s
                LIMIT ? OFFSET ?
                """.formatted(condition, feedbackOrder(order));
        List<AdminFeedbackSummary> content = jdbcTemplate.query(sql,
                (resultSet, rowNum) -> mapSummary(resultSet),
                pageable.getPageSize(), offset(pageable));
        return new PageImpl<>(content, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), total);
    }

    public Optional<AdminFeedbackDetail> findFeedbackDetail(Long feedbackId) {
        List<AdminFeedbackDetail> details = jdbcTemplate.query("""
                        SELECT f.id, f.user_id AS author_id, f.type, f.title, f.content, f.status, f.created_at, f.deleted_at
                        FROM feedbacks f
                        WHERE f.id = ?
                        """,
                (resultSet, rowNum) -> mapDetail(resultSet),
                feedbackId);
        return details.stream().findFirst();
    }

    public List<AdminFeedbackReplySummary> findReplies(Long feedbackId) {
        return jdbcTemplate.query("""
                        SELECT id, content, created_at, updated_at, deleted_at
                        FROM feedback_replies
                        WHERE feedback_id = ?
                        ORDER BY created_at ASC, id ASC
                        """,
                (resultSet, rowNum) -> mapReplySummary(resultSet),
                feedbackId);
    }

    public List<AdminFeedbackAttachmentSummary> findAttachments(Long feedbackId) {
        return jdbcTemplate.query("""
                        SELECT id
                        FROM feedback_attachments
                        WHERE feedback_id = ?
                        ORDER BY id ASC
                        """,
                (resultSet, rowNum) -> new AdminFeedbackAttachmentSummary(resultSet.getLong(ID_COLUMN)),
                feedbackId);
    }

    public Optional<AdminFeedbackAttachmentAccess> findAttachmentForAdmin(Long feedbackId, Long attachmentId) {
        List<AdminFeedbackAttachmentAccess> attachments = jdbcTemplate.query("""
                        SELECT a.id, a.feedback_id, a.file_url, a.original_name
                        FROM feedback_attachments a
                        INNER JOIN feedbacks f ON f.id = a.feedback_id
                        WHERE a.feedback_id = ?
                          AND a.id = ?
                        """,
                (resultSet, rowNum) -> mapAttachmentAccess(resultSet),
                feedbackId,
                attachmentId);
        return attachments.stream().findFirst();
    }

    private AdminFeedbackSummary mapSummary(ResultSet resultSet) throws SQLException {
        return new AdminFeedbackSummary(
                resultSet.getLong(ID_COLUMN),
                FeedbackType.valueOf(resultSet.getString(TYPE_COLUMN)),
                resultSet.getString(TITLE_COLUMN),
                FeedbackStatus.valueOf(resultSet.getString(STATUS_COLUMN)),
                resultSet.getObject(CREATED_AT_COLUMN, LocalDateTime.class),
                resultSet.getBoolean(HAS_REPLIES_COLUMN),
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class),
                resultSet.getObject(AUTHOR_ID_COLUMN, Long.class));
    }

    private AdminFeedbackDetail mapDetail(ResultSet resultSet) throws SQLException {
        return new AdminFeedbackDetail(
                resultSet.getLong(ID_COLUMN),
                FeedbackType.valueOf(resultSet.getString(TYPE_COLUMN)),
                resultSet.getString(TITLE_COLUMN),
                resultSet.getString(CONTENT_COLUMN),
                FeedbackStatus.valueOf(resultSet.getString(STATUS_COLUMN)),
                resultSet.getObject(CREATED_AT_COLUMN, LocalDateTime.class),
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class),
                resultSet.getObject(AUTHOR_ID_COLUMN, Long.class));
    }

    private AdminFeedbackReplySummary mapReplySummary(ResultSet resultSet) throws SQLException {
        return new AdminFeedbackReplySummary(
                resultSet.getLong(ID_COLUMN),
                resultSet.getString(CONTENT_COLUMN),
                resultSet.getObject(CREATED_AT_COLUMN, LocalDateTime.class),
                resultSet.getObject(UPDATED_AT_COLUMN, LocalDateTime.class),
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class) != null,
                resultSet.getObject(DELETED_AT_COLUMN, LocalDateTime.class));
    }

    private AdminFeedbackAttachmentAccess mapAttachmentAccess(ResultSet resultSet) throws SQLException {
        return new AdminFeedbackAttachmentAccess(
                resultSet.getLong(ID_COLUMN),
                resultSet.getLong(FEEDBACK_ID_COLUMN),
                resultSet.getString(FILE_URL_COLUMN),
                resultSet.getString(ORIGINAL_NAME_COLUMN));
    }

    private String deletionCondition(AdminFeedbackDeletionFilter deletionFilter) {
        return switch (deletionFilter) {
            case ALL -> "1 = 1";
            case ACTIVE -> "f.deleted_at IS NULL";
            case DELETED -> "f.deleted_at IS NOT NULL";
        };
    }

    private String feedbackOrder(AdminFeedbackOrder order) {
        return switch (order) {
            case PENDING_FIRST -> "CASE WHEN f.status = 'PENDING' THEN 0 ELSE 1 END, f.created_at DESC, f.id DESC";
            case NEWEST -> "f.created_at DESC, f.id DESC";
            case OLDEST -> "f.created_at ASC, f.id ASC";
        };
    }

    private long offset(Pageable pageable) {
        return (long) pageable.getPageNumber() * pageable.getPageSize();
    }
}
