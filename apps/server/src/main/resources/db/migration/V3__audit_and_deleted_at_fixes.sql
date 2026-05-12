-- 1~3. admin_action_logs, course_reviews, feedback_attachments 테이블 컬럼 추가 (중복 방지용 프로시저 사용)
ALTER TABLE admin_action_logs ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE course_reviews ADD COLUMN deleted_at DATETIME(6) NULL COMMENT '소프트 삭제 일시' AFTER updated_at;
ALTER TABLE feedback_attachments ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)  AFTER created_at;
