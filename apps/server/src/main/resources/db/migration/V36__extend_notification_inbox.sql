ALTER TABLE seat_inbox_items
    MODIFY outbox_id BIGINT NULL,
    MODIFY course_key VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    MODIFY course_name VARCHAR(100) NULL,
    MODIFY available_seats INT NULL,
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'SEAT_OPENED',
    ADD COLUMN source_key VARCHAR(128) NULL,
    ADD COLUMN target_id BIGINT NULL,
    ADD COLUMN title VARCHAR(255) NULL,
    ADD COLUMN message VARCHAR(500) NULL,
    ADD COLUMN available BIT(1) NOT NULL DEFAULT b'1',
    ADD UNIQUE KEY uk_inbox_user_type_source (user_id, type, source_key),
    ADD KEY idx_inbox_type_target (type, target_id),
    ADD KEY idx_inbox_type_source (type, source_key);
