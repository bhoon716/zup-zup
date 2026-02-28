-- 1. 강의 시간표(course_schedules) 테이블 구조 최적화
-- 데이터 정합성 확보를 위해 기존 데이터를 비우고 구조를 변경함 (크롤러가 최신 데이터로 재수집 예정)
TRUNCATE TABLE course_schedules;

-- 기존 period 컬럼 및 인덱스 삭제
SET @period_column_exists := (
    SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = DATABASE() AND table_name = 'course_schedules' AND column_name = 'period'
);
SET @drop_period_column_sql := IF(
    @period_column_exists > 0, 
    'ALTER TABLE course_schedules DROP COLUMN period', 
    'SELECT 1'
);
PREPARE drop_period_column_stmt FROM @drop_period_column_sql;
EXECUTE drop_period_column_stmt;
DEALLOCATE PREPARE drop_period_column_stmt;

SET @course_day_time_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics 
    WHERE table_schema = DATABASE() AND table_name = 'course_schedules' AND index_name = 'idx_course_schedules_course_day_time'
);
SET @drop_course_day_time_index_sql := IF(
    @course_day_time_index_exists > 0, 
    'DROP INDEX idx_course_schedules_course_day_time ON course_schedules', 
    'SELECT 1'
);
PREPARE course_day_time_index_stmt FROM @drop_course_day_time_index_sql;
EXECUTE course_day_time_index_stmt;
DEALLOCATE PREPARE course_day_time_index_stmt;

-- 신규 인덱스 및 제약 조건 구성
CREATE INDEX idx_course_schedules_course_day_time 
ON course_schedules (course_id, day_of_week, start_time, end_time);

SET @time_range_constraint_exists := (
    SELECT COUNT(*) FROM information_schema.table_constraints 
    WHERE constraint_schema = DATABASE() AND table_name = 'course_schedules' AND constraint_name = 'ck_course_schedules_time_range'
);
SET @add_time_range_constraint_sql := IF(
    @time_range_constraint_exists = 0, 
    'ALTER TABLE course_schedules ADD CONSTRAINT ck_course_schedules_time_range CHECK (start_time < end_time)', 
    'SELECT 1'
);
PREPARE add_time_range_constraint_stmt FROM @add_time_range_constraint_sql;
EXECUTE add_time_range_constraint_stmt;
DEALLOCATE PREPARE add_time_range_constraint_stmt;

-- 2. 크롤러 설정(crawler_settings) 테이블 생성 및 초기 데이터 구성
CREATE TABLE IF NOT EXISTS crawler_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_year VARCHAR(4) NOT NULL,
    target_semester VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO crawler_settings (target_year, target_semester)
SELECT '2026', 'U211600010'
WHERE NOT EXISTS (SELECT 1 FROM crawler_settings);

-- 3. 학사 일정(schedules) 관리 테이블 생성
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

-- 4. 사용자 기기(user_devices) 테이블의 token 컬럼 길이 확장
ALTER TABLE user_devices MODIFY COLUMN token VARCHAR(1000) NOT NULL;
ALTER TABLE user_devices MODIFY COLUMN p256dh VARCHAR(500);
ALTER TABLE user_devices MODIFY COLUMN auth VARCHAR(500);

-- 5. 공지사항(announcements) 테이블 생성
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    published TINYINT(1) NOT NULL DEFAULT 1,
    author_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_announcements_public_order
    ON announcements (published, pinned, created_at);

