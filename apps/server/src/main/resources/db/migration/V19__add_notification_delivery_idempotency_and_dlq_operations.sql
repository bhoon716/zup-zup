ALTER TABLE seat_notification_deliveries
  ADD COLUMN idempotency_key CHAR(36) NULL AFTER user_id,
  ADD COLUMN claim_token CHAR(36) NULL AFTER locked_until,
  ADD COLUMN dead_lettered_at DATETIME(6) NULL AFTER last_error,
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

UPDATE seat_notification_deliveries
SET idempotency_key = UUID()
WHERE idempotency_key IS NULL;

UPDATE seat_notification_deliveries
SET dead_lettered_at = NOW(6)
WHERE status = 'DLQ'
  AND dead_lettered_at IS NULL;

ALTER TABLE seat_notification_deliveries
  MODIFY COLUMN idempotency_key CHAR(36) NOT NULL,
  ADD UNIQUE KEY uk_seat_notif_delivery_idempotency_key (idempotency_key),
  ADD KEY idx_seat_notif_delivery_dlq_retention (status, dead_lettered_at);
