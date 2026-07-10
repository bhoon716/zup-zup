CREATE INDEX idx_seat_hist_course_created_id
    ON course_seat_histories (course_key, created_at DESC, id DESC);

CREATE INDEX idx_notif_hist_user_created_id
    ON notification_histories (user_id, created_at DESC, id DESC);
