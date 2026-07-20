CREATE TABLE seat_notification_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_key VARCHAR(64) NOT NULL,
  course_name VARCHAR(100) NOT NULL,
  professor VARCHAR(50) NULL,
  previous_seats INT NOT NULL,
  current_seats INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  version BIGINT NULL,
  created_at DATETIME(6) NULL,
  updated_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_seat_notification_outbox_status_created (status, created_at)
);
