-- 마이그레이션 중 인덱스 조작을 위해 외래 키 체크 일시 중지
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 강의 시간표(course_schedules) 구조 최적화 및 인덱스 재구성
TRUNCATE TABLE course_schedules;

ALTER TABLE course_schedules DROP COLUMN IF EXISTS period;
DROP INDEX IF EXISTS idx_course_schedules_course_day_time ON course_schedules;

ALTER TABLE course_schedules 
    ADD INDEX idx_course_schedules_course_day_time (course_id, day_of_week, start_time, end_time),
    ADD CONSTRAINT ck_course_schedules_time_range CHECK (start_time < end_time);

-- 2. 크롤러 설정 및 학사 일정 관리 테이블 생성
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

-- 3. 공지사항(announcements) 시스템 구축
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

-- 4. 사용자 기기(user_devices) 토큰 길이 조정 및 명세 최적화
ALTER TABLE user_devices 
    MODIFY COLUMN token VARCHAR(500) NOT NULL,
    MODIFY COLUMN p256dh VARCHAR(500),
    MODIFY COLUMN auth VARCHAR(500);

-- 외래 키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;
