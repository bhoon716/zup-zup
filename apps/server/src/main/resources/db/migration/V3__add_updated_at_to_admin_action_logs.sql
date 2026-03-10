-- admin_action_logs 테이블에 누락된 updated_at 컬럼을 추가합니다.
ALTER TABLE admin_action_logs
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
