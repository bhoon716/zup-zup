DROP TABLE IF EXISTS course_review_reactions;
DROP TABLE IF EXISTS course_reviews;

CREATE TABLE course_emoji_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_user_emoji UNIQUE (course_key, user_id, emoji)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
