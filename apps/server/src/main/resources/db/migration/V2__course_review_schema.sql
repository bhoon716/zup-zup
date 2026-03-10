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

-- 피드백 (건의사항 및 버그 리포트) 테이블 생성
CREATE TABLE feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '작성자 ID',
    type VARCHAR(20) NOT NULL COMMENT '분류 (BUG, SUGGESTION, OTHER)',
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    meta_info JSON COMMENT '발생 URL, OS, 브라우저 정보 등 환경 데이터',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '진행 상태 (PENDING, IN_PROGRESS, COMPLETED, REJECTED)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) COMMENT '소프트 삭제 일시',
    INDEX idx_feedback_user (user_id),
    INDEX idx_feedback_status_type (status, type),
    INDEX idx_feedback_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피드백 첨부 이미지 테이블 생성
CREATE TABLE feedback_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    file_url VARCHAR(255) NOT NULL COMMENT '로컬 정적 서빙용 이미지 경로',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_attachment_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피드백 답변(Reply) 관리 테이블 생성 
CREATE TABLE feedback_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL COMMENT '답변 작성자(관리자) ID',
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) COMMENT '소프트 삭제 일시',
    CONSTRAINT fk_reply_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 관리자 액션 로그 테이블 생성
CREATE TABLE admin_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL COMMENT '행위자 관리자 ID',
    action_type VARCHAR(50) NOT NULL COMMENT 'STATUS_CHANGE, REPLY_CREATE, REPLY_UPDATE, FEEDBACK_DELETE 등',
    target_type VARCHAR(20) NOT NULL COMMENT 'FEEDBACK, REPLY',
    target_id BIGINT NOT NULL COMMENT '대상 엔티티 ID',
    meta_data JSON COMMENT '전환된 상태값 등 세부 내역',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
