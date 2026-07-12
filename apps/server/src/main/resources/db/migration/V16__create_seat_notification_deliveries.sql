CREATE TABLE seat_notification_deliveries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  outbox_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  channel VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempts INT NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  locked_until DATETIME(6) NULL,
  last_error VARCHAR(500) NULL,
  created_at DATETIME(6) NULL,
  updated_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_seat_notif_delivery_outbox_user_channel (outbox_id, user_id, channel),
  KEY idx_seat_notif_delivery_ready (status, next_attempt_at),
  CONSTRAINT fk_seat_notif_delivery_outbox FOREIGN KEY (outbox_id) REFERENCES seat_notification_outbox (id)
);
