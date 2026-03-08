-- 강의 리뷰 (Course Review) 테이블 생성
CREATE TABLE course_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT '1부터 5까지의 별점',
    content VARCHAR(255) NOT NULL COMMENT '짧은 코멘트 (리뷰)',
    like_count INT NOT NULL DEFAULT 0 COMMENT '공감 수',
    dislike_count INT NOT NULL DEFAULT 0 COMMENT '비공감 수',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_course_key (course_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 강의 리뷰 공감/비공감 내역 테이블 생성 (중복 투표 방지)
CREATE TABLE course_review_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_review_user (review_id, user_id),
    CONSTRAINT fk_reaction_review FOREIGN KEY (review_id) REFERENCES course_reviews (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- courses 테이블에 리뷰 통계 컬럼 추가
ALTER TABLE courses ADD COLUMN average_rating FLOAT NOT NULL DEFAULT 0.0 COMMENT '평균 별점';
ALTER TABLE courses ADD COLUMN review_count INT NOT NULL DEFAULT 0 COMMENT '리뷰 수';
