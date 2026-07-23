ALTER TABLE course_emoji_reviews
    MODIFY COLUMN emoji VARCHAR(16)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL;
