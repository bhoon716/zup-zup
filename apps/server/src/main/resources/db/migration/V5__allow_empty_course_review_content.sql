DROP PROCEDURE IF EXISTS AllowEmptyCourseReviewContent;
DELIMITER //
CREATE PROCEDURE AllowEmptyCourseReviewContent()
BEGIN
    IF EXISTS (
        SELECT *
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'course_reviews'
          AND COLUMN_NAME = 'content'
          AND IS_NULLABLE = 'NO'
    ) THEN
        ALTER TABLE course_reviews MODIFY COLUMN content VARCHAR(255) NULL COMMENT '짧은 코멘트 (리뷰)';
    END IF;
END //
DELIMITER ;
CALL AllowEmptyCourseReviewContent();
DROP PROCEDURE AllowEmptyCourseReviewContent;
