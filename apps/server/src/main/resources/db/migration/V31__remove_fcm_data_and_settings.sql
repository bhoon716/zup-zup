-- 앱을 중지한 배포 경로에서만 적용한다. FCM 이력/기기/발송 작업과 설정 컬럼을 영구 제거한다.
-- 공유 공석 이벤트는 남은 지원 채널의 상태로 정산하고, 아직 materialize되지 않은 PENDING은 보존한다.
UPDATE seat_notification_outbox o
SET o.status = CASE
        WHEN EXISTS (
            SELECT 1 FROM seat_notification_deliveries d
            WHERE d.outbox_id = o.id AND d.channel <> 'FCM' AND d.status IN ('PENDING', 'PROCESSING')
        ) THEN 'PROCESSING'
        WHEN EXISTS (
            SELECT 1 FROM seat_notification_deliveries d
            WHERE d.outbox_id = o.id AND d.channel <> 'FCM' AND d.status = 'DLQ'
        ) THEN 'DLQ'
        ELSE 'COMPLETED'
    END,
    o.version = COALESCE(o.version, 0) + 1,
    o.updated_at = CURRENT_TIMESTAMP(6)
WHERE o.status IN ('PROCESSING', 'DLQ')
  AND EXISTS (SELECT 1 FROM seat_notification_deliveries d WHERE d.outbox_id = o.id AND d.channel = 'FCM');

-- delivery 삭제 전에 연결을 식별한다. 같은 숫자 ID의 다른 종류 감사 기록은 보존한다.
DELETE a FROM admin_action_logs a
JOIN seat_notification_deliveries d ON d.id = a.target_id
WHERE a.target_type = 'DELIVERY' AND d.channel = 'FCM';

DELETE FROM seat_notification_deliveries WHERE channel = 'FCM';
DELETE FROM notification_histories WHERE channel = 'FCM';
DELETE FROM user_devices WHERE type = 'FCM';

ALTER TABLE user_devices MODIFY COLUMN type ENUM('WEB') NOT NULL;
ALTER TABLE users DROP COLUMN fcm_enabled;
