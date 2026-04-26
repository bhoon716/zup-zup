-- MySQL dump 10.13  Distrib 8.0.45, for Linux (aarch64)
-- Standardized for H2/MySQL Compatibility (Reordered for Foreign Key Constraints)

-- 1. 기초 테이블 (참조하지 않는 테이블)
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `oauth_id` varchar(100) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `oauth_id` (`oauth_id`)
);

CREATE TABLE IF NOT EXISTS `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_key` varchar(64) NOT NULL,
  `subject_code` varchar(20) NOT NULL,
  `class_number` varchar(5) NOT NULL,
  `name` varchar(100) NOT NULL,
  `professor` varchar(50) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `target_grade` varchar(20) DEFAULT NULL,
  `credits` varchar(10) DEFAULT NULL,
  `classification` varchar(50) DEFAULT NULL,
  `academic_year` varchar(4) NOT NULL,
  `semester` varchar(10) NOT NULL,
  `capacity` int NOT NULL,
  `current` int NOT NULL,
  `classroom` varchar(200) DEFAULT NULL,
  `class_time` varchar(500) DEFAULT NULL,
  `class_duration` varchar(50) DEFAULT NULL,
  `general_category` varchar(50) DEFAULT NULL,
  `general_category_by_year` varchar(50) DEFAULT NULL,
  `general_detail` varchar(50) DEFAULT NULL,
  `lecture_hours` int DEFAULT NULL,
  `status` varchar(30) DEFAULT NULL,
  `lecture_language` varchar(30) DEFAULT NULL,
  `grading_method` varchar(30) DEFAULT NULL,
  `accreditation` varchar(30) DEFAULT NULL,
  `disclosure` varchar(30) DEFAULT NULL,
  `disclosure_reason` varchar(100) DEFAULT NULL,
  `course_direction` varchar(500) DEFAULT NULL,
  `has_syllabus` bit(1) DEFAULT NULL,
  `last_crawled_at` datetime(6) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `course_key` (`course_key`)
);

CREATE TABLE IF NOT EXISTS `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT '0',
  `published` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_announcements_public_order` (`published`,`pinned`,`created_at`)
);

CREATE TABLE IF NOT EXISTS `crawler_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_year` varchar(4) NOT NULL,
  `target_semester` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
);

-- 2. 참조 테이블 (외래 키가 있는 테이블)
CREATE TABLE IF NOT EXISTS `timetables` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) NOT NULL,
  `is_main` tinyint(1) NOT NULL DEFAULT '0',
  `academic_year` varchar(4) NOT NULL,
  `semester` varchar(10) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_timetables_user` (`user_id`),
  CONSTRAINT `FK_timetables_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

CREATE TABLE IF NOT EXISTS `custom_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timetable_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `professor` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_custom_schedules_timetable` (`timetable_id`),
  CONSTRAINT `FK_custom_schedules_timetable` FOREIGN KEY (`timetable_id`) REFERENCES `timetables` (`id`)
);

CREATE TABLE IF NOT EXISTS `custom_schedule_times` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `custom_schedule_id` bigint NOT NULL,
  `day_of_week` varchar(10) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `classroom` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_custom_schedule_times_custom` (`custom_schedule_id`),
  CONSTRAINT `FK_custom_schedule_times_custom` FOREIGN KEY (`custom_schedule_id`) REFERENCES `custom_schedules` (`id`)
);

CREATE TABLE IF NOT EXISTS `course_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `day_of_week` varchar(20) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_course_schedules_course_day_time` (`course_id`,`day_of_week`,`start_time`,`end_time`),
  CONSTRAINT `FK_course_schedules_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `ck_course_schedules_time_range` CHECK ((`start_time` < `end_time`))
);

CREATE TABLE IF NOT EXISTS `course_seat_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_key` varchar(64) NOT NULL,
  `capacity` int NOT NULL,
  `current` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seat_hist_course_key` (`course_key`)
);

CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_key` varchar(64) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_notifications_user` (`user_id`),
  CONSTRAINT `FK_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

CREATE TABLE IF NOT EXISTS `notification_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_key` varchar(64) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `channel` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_notification_histories_user` (`user_id`),
  CONSTRAINT `FK_notification_histories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);

CREATE TABLE IF NOT EXISTS `schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_type` varchar(50) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `content` text,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
);
