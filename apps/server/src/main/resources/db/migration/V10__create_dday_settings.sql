CREATE TABLE IF NOT EXISTS `dday_settings`
(
    `id`
    bigint
    NOT
    NULL
    AUTO_INCREMENT,
    `title`
    varchar
(
    255
) NOT NULL,
    `target_date` date NOT NULL,
    `target_time` time DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
)
    );
