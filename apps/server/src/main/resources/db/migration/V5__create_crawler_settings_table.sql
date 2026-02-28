CREATE TABLE IF NOT EXISTS crawler_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_year VARCHAR(4) NOT NULL,
    target_semester VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO crawler_settings (target_year, target_semester)
SELECT '2026', 'U211600010'
WHERE NOT EXISTS (SELECT 1 FROM crawler_settings);
