-- 기존 답변 원문은 제거하되, enum 값뿐인 상태 전환 이력은 안전한 v1 envelope로 보존한다.
UPDATE admin_action_logs
SET meta_data = CASE
    -- DDL 실패 후 Flyway repair/retry가 필요할 때, 앞서 안전하게 바뀐 행을 다시 손상시키지 않는다.
    WHEN JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.schema')) = 'admin-action'
         AND JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.version')) = '1'
         AND JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.event'))
                IN ('STATUS_CHANGED', 'LEGACY_SANITIZED')
        THEN meta_data
    WHEN action_type = 'STATUS_CHANGE'
         AND JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.oldStatus'))
                IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')
         AND JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.newStatus'))
                IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')
        THEN JSON_OBJECT(
            'schema', 'admin-action',
            'version', 1,
            'event', 'STATUS_CHANGED',
            'data', JSON_OBJECT(
                'previousStatus', JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.oldStatus')),
                'newStatus', JSON_UNQUOTE(JSON_EXTRACT(meta_data, '$.newStatus'))
            )
        )
    ELSE JSON_OBJECT(
        'schema', 'admin-action',
        'version', 1,
        'event', 'LEGACY_SANITIZED',
        'data', JSON_OBJECT('reason', 'legacy_metadata_removed')
    )
END;

-- 최신순 pagination과 보존 기간 정리를 위한 인덱스다.
ALTER TABLE admin_action_logs
    MODIFY COLUMN meta_data JSON NOT NULL,
    ADD INDEX idx_admin_action_logs_created_at_id (created_at, id);
