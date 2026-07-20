CREATE INDEX idx_courses_search_scope
    ON courses (academic_year, semester, disclosure, status);
