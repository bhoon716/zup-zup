ALTER TABLE courses
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE course_reviews
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE course_emoji_reviews
    MODIFY COLUMN course_key VARCHAR(64)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
