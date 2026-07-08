CREATE TABLE IF NOT EXISTS course_reviews
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    course_key
    VARCHAR
(
    64
) NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT '1부터 5까지의 별점',
    content VARCHAR
(
    255
) NULL COMMENT '짧은 코멘트 (리뷰)',
    like_count INT NOT NULL DEFAULT 0 COMMENT '공감 수',
    dislike_count INT NOT NULL DEFAULT 0 COMMENT '비공감 수',
    created_at DATETIME
(
    6
) NOT NULL DEFAULT CURRENT_TIMESTAMP
(
    6
),
    updated_at DATETIME
(
    6
) NOT NULL DEFAULT CURRENT_TIMESTAMP
(
    6
),
    deleted_at DATETIME
(
    6
) NULL COMMENT '소프트 삭제 일시',
    INDEX idx_course_key
(
    course_key
)
    );

CREATE TABLE IF NOT EXISTS course_review_reactions
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    review_id
    BIGINT
    NOT
    NULL,
    user_id
    BIGINT
    NOT
    NULL,
    reaction_type
    VARCHAR
(
    20
) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME
(
    6
) NOT NULL DEFAULT CURRENT_TIMESTAMP
(
    6
),
    UNIQUE KEY uk_review_user
(
    review_id,
    user_id
),
    CONSTRAINT fk_reaction_review FOREIGN KEY
(
    review_id
) REFERENCES course_reviews
(
    id
) ON DELETE CASCADE
    );
