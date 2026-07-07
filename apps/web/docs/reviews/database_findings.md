# 데이터베이스 진단 결과 (Database Findings)

## 1. Soft-Delete 필드 인덱싱 누락 (DB-001)
* **상세**: `users` 테이블과 `course_reviews` 테이블 등에 `deleted_at` 컬럼이 추가되었으나 해당 컬럼을 포함하는 인덱싱이 미비합니다.
* **영향**: 활성 사용자 및 정상 리뷰 조회를 위해 주기적으로 수행되는 `deleted_at IS NULL` 조건 쿼리가 누적될 때 성능이 선형적으로 저하됩니다.
* **조치 제안**: 자주 삭제 체크를 진행하는 대상의 경우 인덱스 끝에 `deleted_at`을 포함하도록 마이그레이션 스크립트를 조정해 복합 인덱스 효율을 도모하십시오.

## 2. 외래키(Foreign Key) 누락으로 인한 참조 정합성 위협
* **위치**: [V2__course_review_schema.sql](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/resources/db/migration/V2__course_review_schema.sql)
* **상세**: `course_reviews` 테이블의 `user_id` 및 `course_key` 컬럼은 각각 `users` 테이블과 `courses` 테이블을 논리 참조하지만, 실제 물리 외래키 정의는 이루어지지 않았습니다.
* **영향**: 회원 강제 탈퇴 시 작성되었던 리뷰 데이터가 무결성 제약 없이 테이블 상 고아 데이터로 보존되어 데이터 무결성 결함을 초래합니다.
* **조치 제안**: 외래키 제약조건(`ON DELETE CASCADE` 등)을 추가하여 안전한 데이터 연쇄 소멸 정합성을 확보해 주십시오.
