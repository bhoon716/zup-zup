ALTER TABLE wishlists
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE subscriptions
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE timetable_entries
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE course_seat_histories
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE seat_notification_outbox
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE notification_histories
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
