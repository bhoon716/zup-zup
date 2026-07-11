ALTER TABLE crawler_settings
    ADD COLUMN search_default_semester varchar(20) NULL;

UPDATE crawler_settings
SET search_default_semester = target_semester
WHERE search_default_semester IS NULL;

ALTER TABLE crawler_settings
    MODIFY search_default_semester varchar(20) NOT NULL;
