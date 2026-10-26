ALTER TABLE announcements
    DROP INDEX idx_announcements_public_order;

ALTER TABLE course_seat_histories
    DROP INDEX idx_seat_hist_course_key,
    DROP INDEX idx_seat_hist_course_created_id;

ALTER TABLE notification_histories
    DROP INDEX idx_notif_hist_user_id,
    DROP INDEX idx_notif_hist_user_created_id;

ALTER TABLE subscriptions
    DROP INDEX idx_subscription_course_key,
    DROP INDEX idx_subscription_user_id;

ALTER TABLE wishlists
    DROP INDEX idx_wishlist_user_id,
    DROP INDEX idx_wishlist_course_key;

ALTER TABLE timetables
    DROP INDEX idx_timetable_user_id;

ALTER TABLE user_devices
    DROP INDEX idx_user_device_user_id;

ALTER TABLE course_reviews
    DROP INDEX idx_course_key;

ALTER TABLE feedbacks
    DROP INDEX idx_feedback_user,
    DROP INDEX idx_feedback_status_type,
    DROP INDEX idx_feedback_created_at;

ALTER TABLE courses
    DROP INDEX idx_courses_search_scope,
    DROP INDEX idx_courses_stdtr_no;

ALTER TABLE seat_notification_outbox
    DROP INDEX idx_seat_notification_outbox_status_created;

ALTER TABLE seat_notification_deliveries
    DROP INDEX idx_seat_notif_delivery_ready,
    DROP INDEX idx_seat_notif_delivery_dlq_retention;

ALTER TABLE admin_action_logs
    DROP INDEX idx_admin_action_logs_created_at_id;

ALTER TABLE crawler_run_failures
    DROP INDEX idx_crawler_run_failures_failed_at;
