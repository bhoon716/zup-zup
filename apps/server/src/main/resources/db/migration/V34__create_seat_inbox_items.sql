CREATE TABLE seat_inbox_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  outbox_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  course_key VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  course_name VARCHAR(100) NOT NULL,
  available_seats INT NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  read_at DATETIME(6) NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_seat_inbox_outbox_user (outbox_id, user_id),
  KEY idx_seat_inbox_owner_visible_order (user_id, deleted_at, occurred_at DESC, id DESC)
);
