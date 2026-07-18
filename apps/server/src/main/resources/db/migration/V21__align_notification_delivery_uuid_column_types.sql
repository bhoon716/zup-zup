ALTER TABLE seat_notification_deliveries
    MODIFY COLUMN idempotency_key VARCHAR(36) NOT NULL,
    MODIFY COLUMN claim_token VARCHAR(36) NULL;
