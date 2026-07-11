-- 리뷰 내용(content)을 선택 사항(NULL 허용)으로 변경
ALTER TABLE course_reviews MODIFY content VARCHAR(255) NULL;
