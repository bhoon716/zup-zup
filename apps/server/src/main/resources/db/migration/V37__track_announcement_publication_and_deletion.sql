ALTER TABLE announcements
    ADD COLUMN first_published_at DATETIME(6) NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL;

-- 기존 비공개 공지는 이전에 공개됐는지 확인할 수 없다.
UPDATE announcements
SET first_published_at = COALESCE(created_at, updated_at, CURRENT_TIMESTAMP(6));
