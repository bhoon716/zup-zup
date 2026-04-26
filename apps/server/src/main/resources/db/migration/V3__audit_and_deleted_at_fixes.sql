-- 1~3. admin_action_logs, course_reviews, feedback_attachments 테이블 컬럼 추가 (중복 방지용 프로시저 사용)
DROP PROCEDURE IF EXISTS AddMissingAuditColumns;
DELIMITER //
CREATE PROCEDURE AddMissingAuditColumns()
BEGIN
    -- admin_action_logs: updated_at 추가
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_action_logs' AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE admin_action_logs ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
    END IF;

    -- course_reviews: deleted_at 추가
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_reviews' AND COLUMN_NAME = 'deleted_at') THEN
        ALTER TABLE course_reviews ADD COLUMN deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 일시' AFTER updated_at;
    END IF;

    -- feedback_attachments: updated_at 추가
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feedback_attachments' AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE feedback_attachments ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;
    END IF;
END //
DELIMITER ;
CALL AddMissingAuditColumns();
DROP PROCEDURE AddMissingAuditColumns;

