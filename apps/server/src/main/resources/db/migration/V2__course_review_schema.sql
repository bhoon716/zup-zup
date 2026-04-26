-- 강의 리뷰 (Course Review) 테이블 생성
CREATE TABLE IF NOT EXISTS course_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(255) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_course_key (course_key)
);

-- 강의 리뷰 공감/비공감 내역 테이블 생성 (중복 투표 방지)
CREATE TABLE IF NOT EXISTS course_review_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_review_user (review_id, user_id),
    CONSTRAINT fk_reaction_review FOREIGN KEY (review_id) REFERENCES course_reviews (id) ON DELETE CASCADE
);

-- courses 테이블에 리뷰 통계 컬럼 추가 (표준 SQL)
ALTER TABLE courses ADD COLUMN IF NOT EXISTS average_rating FLOAT NOT NULL DEFAULT 0.0;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0;

-- 피드백 (건의사항 및 버그 리포트) 테이블 생성
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    meta_info JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    INDEX idx_feedback_user (user_id),
    INDEX idx_feedback_status_type (status, type),
    INDEX idx_feedback_created_at (created_at)
);

-- 피드백 첨부 이미지 테이블 생성
CREATE TABLE IF NOT EXISTS feedback_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_attachment_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id) ON DELETE CASCADE
);

-- 피드백 답변(Reply) 관리 테이블 생성 
CREATE TABLE IF NOT EXISTS feedback_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    CONSTRAINT fk_reply_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id) ON DELETE CASCADE
);

-- 관리자 액션 로그 테이블 생성
CREATE TABLE IF NOT EXISTS admin_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    meta_data JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
