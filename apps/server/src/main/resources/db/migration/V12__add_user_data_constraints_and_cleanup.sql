-- Remove duplicate rows before adding stricter constraints.
DELETE
FROM subscriptions
WHERE id IN (SELECT id
             FROM (SELECT id,
                          ROW_NUMBER() OVER (PARTITION BY user_id, course_key ORDER BY id DESC) AS rn
                   FROM subscriptions) ranked
             WHERE ranked.rn > 1);

DELETE
FROM user_devices
WHERE id IN (SELECT id
             FROM (SELECT id,
                          ROW_NUMBER() OVER (PARTITION BY token ORDER BY id DESC) AS rn
                   FROM user_devices) ranked
             WHERE ranked.rn > 1);

DELETE
FROM course_reviews
WHERE id IN (SELECT id
             FROM (SELECT id,
                          ROW_NUMBER() OVER (PARTITION BY user_id, course_key ORDER BY id DESC) AS rn
                   FROM course_reviews) ranked
             WHERE ranked.rn > 1);

UPDATE timetables
SET is_primary = 0
WHERE id IN (SELECT id
             FROM (SELECT id,
                          ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY id DESC) AS rn
                   FROM timetables
                   WHERE is_primary = 1) ranked
             WHERE ranked.rn > 1);

ALTER TABLE subscriptions
    ADD CONSTRAINT uk_subscription_user_course UNIQUE (user_id, course_key);

ALTER TABLE user_devices
    ADD CONSTRAINT uk_user_device_token UNIQUE (token);

ALTER TABLE course_reviews
    ADD CONSTRAINT uk_course_review_user_course UNIQUE (user_id, course_key);

ALTER TABLE timetables
    ADD COLUMN primary_user_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN is_primary = 1 THEN user_id
            ELSE NULL
            END
        ) STORED;

ALTER TABLE timetables
    ADD CONSTRAINT uk_timetable_primary_user UNIQUE (primary_user_id);
