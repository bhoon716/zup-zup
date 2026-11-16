CREATE TABLE subscription_ends (
    user_id BIGINT NOT NULL,
    course_key VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, course_key)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
