-- 🚀 [V3] 데이터베이스 구조 최적화 및 초기화 (Idempotent 및 신규 필드 반영 버전)
-- 중요 유저 데이터(Users, Subscriptions, Timetables, Wishlists)는 보존하되, 
-- 구조가 대폭 변경된 관리 테이블 및 빈 테이블들은 초기화하여 충돌을 방지합니다.

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 대규모/구조변경/관리 테이블 초기화
DROP TABLE IF EXISTS course_seat_histories;
DROP TABLE IF EXISTS course_schedules;
DROP TABLE IF EXISTS notification_histories;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS crawler_settings;
DROP TABLE IF EXISTS schedules;
DROP TABLE IF EXISTS announcements;
DROP TABLE IF EXISTS custom_schedule_times;
DROP TABLE IF EXISTS custom_schedules;

-- 2. 핵심 마스터 테이블(courses) 재생성
CREATE TABLE courses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) NOT NULL UNIQUE,
    subject_code VARCHAR(20) NOT NULL,
    class_number VARCHAR(5) NOT NULL,
    name VARCHAR(100) NOT NULL,
    professor VARCHAR(50),
    department VARCHAR(100),
    target_grade VARCHAR(20),
    credits VARCHAR(10),
    classification VARCHAR(50),
    academic_year VARCHAR(4) NOT NULL,
    semester VARCHAR(10) NOT NULL,
    capacity INT NOT NULL,
    current INT NOT NULL,
    classroom VARCHAR(200),
    class_time VARCHAR(500),
    class_duration VARCHAR(50),
    general_category VARCHAR(50),
    general_category_by_year VARCHAR(50),
    general_detail VARCHAR(50),
    lecture_hours INT,
    status VARCHAR(30),
    lecture_language VARCHAR(30),
    grading_method VARCHAR(30),
    accreditation VARCHAR(30),
    disclosure VARCHAR(30),
    disclosure_reason VARCHAR(100),
    course_direction VARCHAR(500),
    has_syllabus BIT(1),
    last_crawled_at DATETIME(6) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 강의 시간표(course_schedules) 재생성
CREATE TABLE course_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    INDEX idx_course_schedules_course_day_time (course_id, day_of_week, start_time, end_time),
    CONSTRAINT ck_course_schedules_time_range CHECK (start_time < end_time),
    CONSTRAINT FK_course_schedules_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 여석 이력(course_seat_histories) 재생성
CREATE TABLE course_seat_histories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) NOT NULL,
    capacity INT NOT NULL,
    current INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    INDEX idx_seat_hist_course_key (course_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 알림 기록(notification_histories) 재생성
CREATE TABLE notification_histories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_key VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    INDEX idx_notif_hist_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 커스텀 스케줄 테이블 (신규 구조 반영)
CREATE TABLE custom_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    timetable_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    professor VARCHAR(50),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT FK_custom_schedules_timetable FOREIGN KEY (timetable_id) REFERENCES timetables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE custom_schedule_times (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    custom_schedule_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    classroom VARCHAR(100),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT FK_custom_schedule_times_custom FOREIGN KEY (custom_schedule_id) REFERENCES custom_schedules (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 관리용 테이블 재생성
CREATE TABLE crawler_settings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    target_year VARCHAR(4) NOT NULL,
    target_semester VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO crawler_settings (target_year, target_semester) VALUES ('2026', 'U211600010');

CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_announcements_public_order ON announcements (published, pinned, created_at);

-- 8. 사용자 기기 최적화 및 제약조건 복구
ALTER TABLE user_devices 
    MODIFY COLUMN token VARCHAR(500) NOT NULL,
    MODIFY COLUMN p256dh VARCHAR(500),
    MODIFY COLUMN auth VARCHAR(500);

SET FOREIGN_KEY_CHECKS = 1;
