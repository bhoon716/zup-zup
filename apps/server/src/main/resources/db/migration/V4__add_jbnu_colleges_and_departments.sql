-- 2026학년도 전북대학교 단과대학 및 학과 데이터 추가

-- 1. 단과대학 테이블 생성
CREATE TABLE IF NOT EXISTS `colleges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_college_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. 학과 테이블 생성
CREATE TABLE IF NOT EXISTS `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `college_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_departments_college` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. 단과대학 데이터 삽입
INSERT IGNORE INTO `colleges` (id, name) VALUES 
(1, '간호대학'), (2, '경상대학'), (3, '공과대학'), (4, '농업생명과학대학'), 
(5, '본부직속'), (6, '사범대학'), (7, '사회과학대학'), (8, '생활과학대학'), 
(9, '수의과대학'), (10, '약학대학'), (11, '예술대학'), (12, '의과대학'), 
(13, '인문대학'), (14, '자연과학대학'), (15, '치과대학'), (16, '환경생명자원대학'), 
(17, 'AI대학');

-- 4. 학과 데이터 삽입
INSERT IGNORE INTO `departments` (college_id, name) VALUES
-- 간호대학
(1, '간호학과'),
-- 경상대학
(2, '경영학과'), (2, '경제학부'), (2, '무역학과'), (2, '회계학과'),
-- 공과대학
(3, '건축공학과'), (3, '고분자·나노공학과'), (3, '유기소재섬유공학과'), (3, '기계공학과'), (3, '기계설계공학부'), 
(3, '기계시스템공학부'), (3, '도시공학과'), (3, '바이오메디컬공학부'), (3, '산업정보시스템공학과'), (3, '신소재공학부'), 
(3, '양자시스템공학과'), (3, '융합기술공학부'), (3, '전기공학과'), (3, '전자공학부'), 
(3, '토목/환경/자원·에너지공학부'), (3, '항공우주공학과'), (3, '화학공학부'),
-- 농업생명과학대학
(4, '농경제유통학부'), (4, '식물의학과'), (4, '동물생명공학과'), (4, '동물자원과학과'), (4, '목재응용과학과'), 
(4, '산림환경과학과'), (4, '생명자원융합학과'), (4, '생물산업기계공학과'), (4, '생물환경화학과'), (4, '식품공학과'), 
(4, '원예학과'), (4, '작물생명과학과'), (4, '조경학과'), (4, '지역건설공학과'), (4, '스마트팜학과'),
-- 본부직속
(5, '융합학부'), (5, '국제이공학부'), (5, '융합자율전공학부'), (5, '한옥학과'), (5, '글로컬커머스학과'), 
(5, '한국어학과'), (5, 'K-엔터테인먼트학과'), (5, '이차전지공학과'), (5, '첨단방위산업학과'),
-- 사범대학
(6, '국어교육과'), (6, '과학교육학부'), (6, '교육학과'), (6, '독어교육과'), (6, '사회과교육학부'), 
(6, '수학교육과'), (6, '영어교육과'), (6, '체육교육과'),
-- 사회과학대학
(7, '사회복지학과'), (7, '사회학과'), (7, '미디어커뮤니케이션학과'), (7, '심리학과'), (7, '정치외교학과'), (7, '행정학과'), (7, '공공인재학부'),
-- 생활과학대학
(8, '식품영양학과'), (8, '아동학과'), (8, '의류학과'), (8, '주거환경학과'),
-- 수의과대학
(9, '수의예과'), (9, '수의학과'),
-- 약학대학
(10, '약학과'),
-- 예술대학
(11, '무용학과'), (11, '미술학과'), (11, '산업디자인학과'), (11, '음악과'), (11, '한국음악학과'),
-- 의과대학
(12, '의예과'),
-- 인문대학
(13, '고고문화인류학과'), (13, '국어국문학과'), (13, '독일학과'), (13, '문헌정보학과'), (13, '사학과'), 
(13, '스페인·중남미학과'), (13, '영어영문학과'), (13, '일본학과'), (13, '중어중문학과'), (13, '철학과'), 
(13, '프랑스·아프리카학과'), (13, '국제학부'),
-- 자연과학대학
(14, '과학학과'), (14, '물리학과'), (14, '반도체과학기술학과'), (14, '분자생물학과'), (14, '생명과학과'), 
(14, '수학과'), (14, '스포츠과학과'), (14, '지구환경과학과'), (14, '통계학과'), (14, '화학과'),
-- 치과대학
(15, '치의예과'),
-- 환경생명자원대학
(16, '생명공학부'), (16, '생태조경디자인학과'), (16, '한약자원학과'),
-- AI대학
(17, '소프트웨어공학과'), (17, '컴퓨터인공지능학부');

-- 5. 기존 강의 테이블 구조 확장
ALTER TABLE `courses` ADD COLUMN `college_id` bigint DEFAULT NULL;
ALTER TABLE `courses` ADD COLUMN `department_id` bigint DEFAULT NULL;

-- 6. 기존 데이터 매핑 (문자열 일치 기반)
-- 정확히 일치하는 학과부터 우선 매핑
UPDATE `courses` c
JOIN `departments` d ON c.department = d.name
SET c.department_id = d.id, c.college_id = d.college_id;

-- 쉼표로 구분된 복수 학과 데이터 매핑 (아직 매핑되지 않은 건에 대해)
UPDATE `courses` c
JOIN `departments` d ON FIND_IN_SET(d.name, REPLACE(c.department, ', ', ',')) > 0
SET c.department_id = d.id, c.college_id = d.college_id
WHERE c.department_id IS NULL;

-- 7. 검색 성능 향상을 위한 인덱스 추가
CREATE INDEX `idx_courses_college_id` ON `courses`(`college_id`);
CREATE INDEX `idx_courses_department_id` ON `courses`(`department_id`);
