-- admin_action_logs: updated_at 추가
ALTER TABLE admin_action_logs ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- course_reviews: deleted_at 추가
ALTER TABLE course_reviews ADD COLUMN IF NOT EXISTS deleted_at DATETIME(6) NULL;

-- feedback_attachments: updated_at 추가
ALTER TABLE feedback_attachments ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
