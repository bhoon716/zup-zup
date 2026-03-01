-- 🚀 [V3] 데이터베이스 구조 최적화 및 레거시 데이터 초기화 스크립트
-- 이 서비스의 핵심인 Users, Subscriptions, Timetables, Wishlists 등 유저 생성 데이터는 100% 보존합니다.

-- 마이그레이션 중 제약 조건 충돌 방지를 위해 외래 키 체크 일시 중지
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 대규모/불필요 레거시 데이터 및 테이블 초기화
-- 재수집 및 재생성 가능한 데이터들을 삭제하여 DB 용량 최적화 및 인덱스 꼬임 방지
DROP TABLE IF EXISTS course_seat_histories;
DROP TABLE IF EXISTS course_schedules;
DROP TABLE IF EXISTS notification_histories;
DROP TABLE IF EXISTS courses;

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
    classification ENUM('BASIC_REQUIRED','GENERAL_EDUCATION','GENERAL_ELECTIVE','MAJOR','MAJOR_ELECTIVE','MAJOR_REQUIRED','MILITARY_SCIENCE','PREREQUISITE','SERIES_COMMON','TEACHING_PROFESSION','TEACHING_PROFESSION_GRAD'),
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
    status ENUM('BLENDED','FIELD_TRAINING','FLIPPED_LEARNING','GENERAL','ONLINE_OFFLINE','REMOTE_CONTENTS','REMOTE_REALTIME','SOCIAL_SERVICE','SPECIAL_ENGLISH','THESIS_RESEARCH','VIDEO_CONFERENCE'),
    lecture_language ENUM('CHINESE','ENGLISH','FRENCH','GERMAN','JAPANESE','KOREAN','SPANISH'),
    grading_method ENUM('ABSOLUTE','ETC_LAW_SCHOOL','PASS_FAIL','RELATIVE_1','RELATIVE_2','RELATIVE_3'),
    accreditation ENUM('ENGINEERING','GENERAL','MANAGEMENT','NURSING'),
    disclosure ENUM('PRIVATE','PUBLIC'),
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

-- 6. 기타 관리 테이블 생성
CREATE TABLE IF NOT EXISTS crawler_settings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    target_year VARCHAR(4) NOT NULL,
    target_semester VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO crawler_settings (target_year, target_semester)
SELECT '2026', 'U211600010'
WHERE NOT EXISTS (SELECT 1 FROM crawler_settings);

CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_announcements_public_order ON announcements (published, pinned, created_at);

-- 7. 사용자 기기(user_devices) 사양 최적화 (데이터 유지)
ALTER TABLE user_devices 
    MODIFY COLUMN token VARCHAR(500) NOT NULL,
    MODIFY COLUMN p256dh VARCHAR(500),
    MODIFY COLUMN auth VARCHAR(500);

-- 외래 키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;
