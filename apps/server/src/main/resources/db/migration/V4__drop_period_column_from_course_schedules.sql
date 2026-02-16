SET @period_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'course_schedules'
      AND column_name = 'period'
);
SET @drop_period_column_sql := IF(
    @period_column_exists > 0,
    'ALTER TABLE course_schedules DROP COLUMN period',
    'SELECT 1'
);
PREPARE drop_period_column_stmt FROM @drop_period_column_sql;
EXECUTE drop_period_column_stmt;
DEALLOCATE PREPARE drop_period_column_stmt;

SET @time_range_constraint_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'course_schedules'
      AND constraint_name = 'ck_course_schedules_time_range'
);
SET @add_time_range_constraint_sql := IF(
    @time_range_constraint_exists = 0,
    'ALTER TABLE course_schedules ADD CONSTRAINT ck_course_schedules_time_range CHECK (start_time < end_time)',
    'SELECT 1'
);
PREPARE add_time_range_constraint_stmt FROM @add_time_range_constraint_sql;
EXECUTE add_time_range_constraint_stmt;
DEALLOCATE PREPARE add_time_range_constraint_stmt;
