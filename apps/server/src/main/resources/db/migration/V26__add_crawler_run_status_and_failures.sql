ALTER TABLE courses
    MODIFY COLUMN last_crawled_at DATETIME(6) NULL;

CREATE TABLE crawler_status (
    id BIGINT NOT NULL,
    latest_status VARCHAR(20) NOT NULL,
    last_success_at DATETIME(6) NULL,
    last_failure_at DATETIME(6) NULL,
    last_failure_stage VARCHAR(30) NULL,
    last_failure_type VARCHAR(100) NULL,
    last_failure_message VARCHAR(500) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_crawler_status_singleton CHECK (id = 1)
) ENGINE=InnoDB;

CREATE TABLE crawler_run_failures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    failed_at DATETIME(6) NOT NULL,
    failure_stage VARCHAR(30) NOT NULL,
    failure_type VARCHAR(100) NOT NULL,
    failure_message VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_crawler_run_failures_failed_at (failed_at)
) ENGINE=InnoDB;
