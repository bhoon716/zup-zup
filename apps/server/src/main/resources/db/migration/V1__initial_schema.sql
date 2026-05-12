SET REFERENTIAL_INTEGRITY FALSE;
SET FOREIGN_KEY_CHECKS=0;

-- `announcements` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200)  NOT NULL,
  `content` text  NOT NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  `published` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_announcements_public_order` (`published`,`pinned`,`created_at`)
);

--
-- `crawler_settings` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `crawler_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_year` varchar(4)  NOT NULL,
  `target_semester` varchar(20)  NOT NULL,
  PRIMARY KEY (`id`)
);

--
-- `schedules` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_type` varchar(50)  NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

--
-- `users` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `discord_enabled` bit(1) NOT NULL,
  `discord_id` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `email_enabled` bit(1) NOT NULL,
  `fcm_enabled` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  `notification_email` varchar(255) DEFAULT NULL,
  `onboarding_completed` bit(1) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `web_push_enabled` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
);

--
-- `courses` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_key` varchar(64)  NOT NULL,
  `subject_code` varchar(20)  NOT NULL,
  `class_number` varchar(5)  NOT NULL,
  `name` varchar(100)  NOT NULL,
  `professor` varchar(50)  DEFAULT NULL,
  `department` varchar(100)  DEFAULT NULL,
  `target_grade` varchar(20)  DEFAULT NULL,
  `credits` varchar(10)  DEFAULT NULL,
  `classification` varchar(50)  DEFAULT NULL,
  `academic_year` varchar(4)  NOT NULL,
  `semester` varchar(10)  NOT NULL,
  `capacity` int NOT NULL,
  `current` int NOT NULL,
  `classroom` varchar(200)  DEFAULT NULL,
  `class_time` varchar(500)  DEFAULT NULL,
  `class_duration` varchar(50)  DEFAULT NULL,
  `general_category` varchar(50)  DEFAULT NULL,
  `general_category_by_year` varchar(50)  DEFAULT NULL,
  `general_detail` varchar(50)  DEFAULT NULL,
  `lecture_hours` int DEFAULT NULL,
  `status` varchar(30)  DEFAULT NULL,
  `lecture_language` varchar(30)  DEFAULT NULL,
  `grading_method` varchar(30)  DEFAULT NULL,
  `accreditation` varchar(30)  DEFAULT NULL,
  `disclosure` varchar(30)  DEFAULT NULL,
  `disclosure_reason` varchar(100)  DEFAULT NULL,
  `course_direction` varchar(500)  DEFAULT NULL,
  `has_syllabus` bit(1) DEFAULT NULL,
  `last_crawled_at` datetime(6) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `course_key` (`course_key`)
);

--
-- `course_schedules` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `course_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `day_of_week` varchar(20)  NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_course_schedules_course_day_time` (`course_id`,`day_of_week`,`start_time`,`end_time`),
  CONSTRAINT `FK_course_schedules_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `ck_course_schedules_time_range` CHECK ((`start_time` < `end_time`))
);

--
-- `course_seat_histories` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `course_seat_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_key` varchar(64)  NOT NULL,
  `capacity` int NOT NULL,
  `current` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seat_hist_course_key` (`course_key`)
);

--
-- `timetables` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `timetables` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_primary` bit(1) NOT NULL,
  `name` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_timetable_user_id` (`user_id`)
);

--
-- `timetable_entries` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `timetable_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `course_key` varchar(64) NOT NULL,
  `timetable_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_timetable_course` (`timetable_id`,`course_key`),
  CONSTRAINT `FKmscrehkt2k9dvuupu038muvxh` FOREIGN KEY (`timetable_id`) REFERENCES `timetables` (`id`)
);

--
-- `custom_schedules` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `custom_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timetable_id` bigint NOT NULL,
  `title` varchar(100)  NOT NULL,
  `professor` varchar(50)  DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_custom_schedules_timetable` (`timetable_id`),
  CONSTRAINT `FK_custom_schedules_timetable` FOREIGN KEY (`timetable_id`) REFERENCES `timetables` (`id`)
);

--
-- `custom_schedule_times` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `custom_schedule_times` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `custom_schedule_id` bigint NOT NULL,
  `day_of_week` varchar(10)  NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `classroom` varchar(100)  DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_custom_schedule_times_custom` (`custom_schedule_id`),
  CONSTRAINT `FK_custom_schedule_times_custom` FOREIGN KEY (`custom_schedule_id`) REFERENCES `custom_schedules` (`id`)
);

--
-- `notification_histories` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `notification_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_key` varchar(64)  NOT NULL,
  `title` varchar(255)  NOT NULL,
  `message` text  NOT NULL,
  `channel` varchar(20)  NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notif_hist_user_id` (`user_id`)
);

--
-- `subscriptions` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `subscriptions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `course_key` varchar(64) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_subscription_course_key` (`course_key`),
  KEY `idx_subscription_user_id` (`user_id`)
);

--
-- `user_devices` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `user_devices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `alias` varchar(255) DEFAULT NULL,
  `auth` varchar(500) DEFAULT NULL,
  `p256dh` varchar(500) DEFAULT NULL,
  `token` varchar(500) NOT NULL,
  `type` enum('FCM','WEB') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_device_user_id` (`user_id`)
);

--
-- `wishlists` 테이블 구조
--

CREATE TABLE IF NOT EXISTS `wishlists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `course_key` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wishlist_user_course` (`user_id`,`course_key`),
  KEY `idx_wishlist_user_id` (`user_id`),
  KEY `idx_wishlist_course_key` (`course_key`)
);

-- 덤프 완료: 2026-03-03 22:20:25

SET REFERENTIAL_INTEGRITY TRUE;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO crawler_settings (target_year, target_semester) VALUES ('2026', 'U211600010');
