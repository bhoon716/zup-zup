ALTER TABLE courses
    ADD COLUMN stdtr_no VARCHAR(50) NULL AFTER subject_code;

CREATE INDEX idx_courses_stdtr_no
    ON courses (stdtr_no);
