CREATE TABLE IF NOT EXISTS course_schedules_legacy_backup LIKE course_schedules;
TRUNCATE TABLE course_schedules_legacy_backup;
INSERT INTO course_schedules_legacy_backup
SELECT *
FROM course_schedules;

CREATE TABLE IF NOT EXISTS course_schedules_migrated (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    day_of_week VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_course_schedules_migrated_course_day_time (course_id, day_of_week, start_time, end_time)
);

TRUNCATE TABLE course_schedules_migrated;

INSERT INTO course_schedules_migrated (course_id, day_of_week, start_time, end_time)
SELECT
    merged_ranges.course_id,
    merged_ranges.day_of_week,
    merged_ranges.start_time,
    merged_ranges.end_time
FROM (
    SELECT
        grouped_rows.course_id,
        grouped_rows.day_of_week,
        MIN(grouped_rows.start_time) AS start_time,
        MAX(grouped_rows.end_time) AS end_time
    FROM (
        SELECT
            ordered_rows.id,
            ordered_rows.course_id,
            ordered_rows.day_of_week,
            ordered_rows.start_time,
            ordered_rows.end_time,
            SUM(CASE WHEN ordered_rows.prev_end = ordered_rows.start_time THEN 0 ELSE 1 END) OVER (
                PARTITION BY ordered_rows.course_id, ordered_rows.day_of_week
                ORDER BY ordered_rows.start_time, ordered_rows.end_time, ordered_rows.id
            ) AS group_no
        FROM (
            SELECT
                cs.id,
                cs.course_id,
                cs.day_of_week,
                cs.start_time,
                cs.end_time,
                LAG(cs.end_time) OVER (
                    PARTITION BY cs.course_id, cs.day_of_week
                    ORDER BY cs.start_time, cs.end_time, cs.id
                ) AS prev_end
            FROM course_schedules cs
        ) ordered_rows
    ) grouped_rows
    GROUP BY grouped_rows.course_id, grouped_rows.day_of_week, grouped_rows.group_no
) merged_ranges
GROUP BY merged_ranges.course_id, merged_ranges.day_of_week, merged_ranges.start_time, merged_ranges.end_time;

TRUNCATE TABLE course_schedules;
INSERT INTO course_schedules (course_id, day_of_week, start_time, end_time)
SELECT
    course_id,
    day_of_week,
    start_time,
    end_time
FROM course_schedules_migrated;

DROP TABLE course_schedules_migrated;

SET @course_day_time_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'course_schedules'
      AND index_name = 'idx_course_schedules_course_day_time'
);
SET @drop_course_day_time_index_sql := IF(
    @course_day_time_index_exists > 0,
    'DROP INDEX idx_course_schedules_course_day_time ON course_schedules',
    'SELECT 1'
);
PREPARE course_day_time_index_stmt FROM @drop_course_day_time_index_sql;
EXECUTE course_day_time_index_stmt;
DEALLOCATE PREPARE course_day_time_index_stmt;

CREATE INDEX idx_course_schedules_course_day_time
    ON course_schedules (course_id, day_of_week, start_time, end_time);
