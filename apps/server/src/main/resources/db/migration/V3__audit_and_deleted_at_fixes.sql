-- 1. admin_action_logs 테이블에 updated_at 컬럼 추가
ALTER TABLE admin_action_logs
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- 2. course_reviews 테이블에 deleted_at 컬럼 추가
ALTER TABLE course_reviews
    ADD COLUMN deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 일시' AFTER updated_at;

-- 3. feedback_attachments 테이블에 updated_at 컬럼 추가
ALTER TABLE feedback_attachments
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;
