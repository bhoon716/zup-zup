# DB 성능 인덱스 제거

기준: `1c3cb781f` 이후, 사용자 2026-10-26 전체 제거 선택. 운영 DB는 열거나 변경하지 않는다.

## 제거 대상

V30은 아래 22개 성능용 비유니크 인덱스만 제거한다. 관련 쿼리는 유지하며 조회 비용과 잠금 대기가 증가할 수 있다.

| 테이블 | 제거 인덱스 | 기존 대상 쿼리 |
| --- | --- | --- |
| `announcements` | `idx_announcements_public_order` | 공지 공개 목록 정렬 |
| `course_seat_histories` | `idx_seat_hist_course_key` | 강의 좌석 이력 |
| `course_seat_histories` | `idx_seat_hist_course_created_id` | 강의 좌석 이력 최신순 |
| `notification_histories` | `idx_notif_hist_user_id` | 사용자 알림 이력 |
| `notification_histories` | `idx_notif_hist_user_created_id` | 사용자 알림 이력 최신순 |
| `subscriptions` | `idx_subscription_course_key` | 빈자리 알림 수신 구독 |
| `subscriptions` | `idx_subscription_user_id` | 사용자 구독 목록 |
| `wishlists` | `idx_wishlist_user_id` | 사용자 관심 목록 |
| `wishlists` | `idx_wishlist_course_key` | 인기 강의 집계 |
| `timetables` | `idx_timetable_user_id` | 사용자 시간표 목록 |
| `user_devices` | `idx_user_device_user_id` | 사용자 알림 기기 목록 |
| `course_reviews` | `idx_course_key` | 강의 리뷰 조회·집계 |
| `feedbacks` | `idx_feedback_user` | 사용자 문의 목록 |
| `feedbacks` | `idx_feedback_status_type` | 현재 저장소의 필터 조회 사용 없음 |
| `feedbacks` | `idx_feedback_created_at` | 관리자 문의 최신순 |
| `courses` | `idx_courses_search_scope` | 강의 검색 범위 |
| `courses` | `idx_courses_stdtr_no` | 강의 번호 검색 |
| `seat_notification_outbox` | `idx_seat_notification_outbox_status_created` | 순차 outbox 조회·잠금 |
| `seat_notification_deliveries` | `idx_seat_notif_delivery_ready` | 준비된 발송 선점 |
| `seat_notification_deliveries` | `idx_seat_notif_delivery_dlq_retention` | DLQ 조회·보존 기간 정리 |
| `admin_action_logs` | `idx_admin_action_logs_created_at_id` | 감사 기록 조회·보존 기간 정리 |
| `crawler_run_failures` | `idx_crawler_run_failures_failed_at` | 현재 저장소의 조회 사용 없음 |

엔티티 8개의 성능용 `@Index` 11개를 제거한다. `user_devices.idx_user_device_token` 선언은 실제 Flyway 스키마에 없으며, 실제 `uk_user_device_token` 유니크 제약은 유지한다.

## 유지 대상과 이유

- 모든 `PRIMARY`: 행 식별·JPA 식별·참조 무결성.
- `users.UK6dotkott2kjsp8vw4d0m25fb7`(email), `uk_users_oauth_identity`: 계정·외부 로그인 식별자 중복 방지.
- `courses.course_key`: 강의 식별자 중복 방지.
- `uk_timetable_course`, `uk_wishlist_user_course`, `uk_subscription_user_course`: 중복 강의·관심·구독 방지. 첫 번째는 시간표 FK도 지원.
- `uk_course_user_emoji`, `uk_review_user`, `uk_course_review_user_course`: 이모지·반응·리뷰 중복 방지. `uk_review_user`는 리뷰 FK도 지원.
- `uk_user_device_token`: 동일 단말 토큰 중복 방지.
- `uk_timetable_primary_user`: 사용자당 대표 시간표 하나 보장.
- `uk_seat_notif_delivery_outbox_user_channel`, `uk_seat_notif_delivery_idempotency_key`: 발송 작업 중복 생성·idempotency key 중복 방지. 첫 번째는 outbox FK도 지원.
- `idx_course_schedules_course_day_time`: `FK_course_schedules_course`를 지원하는 기존 인덱스. 현재 FK를 위해 필요한 인덱스이므로 유지하며 성능 목적의 새 인덱스를 만들지 않는다.
- `FK_custom_schedules_timetable`, `FK_custom_schedule_times_custom`: 시간표·사용자 일정 FK 지원.
- `fk_attachment_feedback`, `fk_reply_feedback`: 문의 첨부·답변 FK를 MySQL이 지원하기 위해 생성한 인덱스.
- Spring Batch `JOB_INST_UN`, 세 시퀀스 테이블의 `UNIQUE_KEY_UN`: Job 식별·시퀀스 계약. `JOB_INST_EXEC_FK`, `JOB_EXEC_PARAMS_FK`, `JOB_EXEC_STEP_FK` 지원 인덱스와 `STEP_EXEC_CTX_FK`, `JOB_EXEC_CTX_FK`를 지원하는 기존 PK도 유지한다. 별도 테이블 제거는 이번 범위가 아니다.
- `flyway_schema_history_s_idx`: Flyway가 생성·관리하는 이력 인덱스. 애플리케이션 성능 최적화용 선언과 구분한다.
- Redis Indexed Session의 principal→session 인덱스: 탈퇴 시 모든 세션 회수 기능. MySQL 인덱스와 별개이며 변경하지 않는다.

감사·수집 실패 엔티티나 데이터는 삭제하지 않는다. 인덱스만 제거한다.

## 적용·복구 한계

V1–V29의 파일과 checksum을 바꾸지 않는다. 운영 적용 전 백업, 실제 스키마와 이름·FK·Flyway 이력 대조, DDL metadata lock과 스캔/잠금 쿼리 부하 점검이 필요하다. MySQL DDL은 암묵적 커밋하므로 V30 전체 원자적 rollback을 보장하지 않는다. 운영 데이터 삭제는 없지만 인덱스 복구 중 자원·잠금 비용은 발생할 수 있다.

아래 SQL은 적용 전 상태로 돌아갈 때의 수동 검토용이다. 운영 자동 실행 또는 Flyway down migration으로 등록하지 않는다. 실패 지점의 실제 존재 여부를 확인하고 없는 항목만 복구한 뒤 Flyway 이력을 검토해야 한다. 개발 DB의 검증을 운영 성능 측정으로 해석하지 않는다.

```sql
CREATE INDEX idx_announcements_public_order ON announcements (published, pinned, created_at);
CREATE INDEX idx_seat_hist_course_key ON course_seat_histories (course_key);
CREATE INDEX idx_seat_hist_course_created_id ON course_seat_histories (course_key, created_at DESC, id DESC);
CREATE INDEX idx_notif_hist_user_id ON notification_histories (user_id);
CREATE INDEX idx_notif_hist_user_created_id ON notification_histories (user_id, created_at DESC, id DESC);
CREATE INDEX idx_subscription_course_key ON subscriptions (course_key);
CREATE INDEX idx_subscription_user_id ON subscriptions (user_id);
CREATE INDEX idx_wishlist_user_id ON wishlists (user_id);
CREATE INDEX idx_wishlist_course_key ON wishlists (course_key);
CREATE INDEX idx_timetable_user_id ON timetables (user_id);
CREATE INDEX idx_user_device_user_id ON user_devices (user_id);
CREATE INDEX idx_course_key ON course_reviews (course_key);
CREATE INDEX idx_feedback_user ON feedbacks (user_id);
CREATE INDEX idx_feedback_status_type ON feedbacks (status, type);
CREATE INDEX idx_feedback_created_at ON feedbacks (created_at);
CREATE INDEX idx_courses_search_scope ON courses (academic_year, semester, disclosure, status);
CREATE INDEX idx_courses_stdtr_no ON courses (stdtr_no);
CREATE INDEX idx_seat_notification_outbox_status_created ON seat_notification_outbox (status, created_at);
CREATE INDEX idx_seat_notif_delivery_ready ON seat_notification_deliveries (status, next_attempt_at);
CREATE INDEX idx_seat_notif_delivery_dlq_retention ON seat_notification_deliveries (status, dead_lettered_at);
CREATE INDEX idx_admin_action_logs_created_at_id ON admin_action_logs (created_at, id);
CREATE INDEX idx_crawler_run_failures_failed_at ON crawler_run_failures (failed_at);
```

## 검증

격리 MySQL 8.4에서 fresh migration과 V29→V30 업그레이드, PK·유니크·FK 메타데이터 보존, 데이터 보존·제약 위반 거절·이력 정렬·복구 DDL을 검증한다. 최종 실행 결과는 완료 후 기록한다. 실제 운영 부하와 p95/p99·잠금 대기는 미측정이다.

### 최종 실행 기록 — 2026-10-26

- 격리 MySQL 8.4의 fresh schema와 V29→V30 업그레이드 2건 통과. 제거 22개, 성능 전용 비유니크 인덱스 잔존 없음, PK·유니크·FK 메타데이터 동일함을 확인했다.
- 동일 시각 이력의 ID 내림차순·30개 페이징·사용자 범위, 구독/관심/발송/idempotency 중복 거절, FK 위반·부모 삭제 거절, 준비된 발송 조회와 선점 토큰 불일치 정산 거절을 확인했다. 실제 동시 부하의 잠금 범위/대기는 측정하지 않았다.
- 기존 인덱스 정의로 복구한 뒤 이름·컬럼 순서·DESC 방향과 제약 메타데이터가 이전 상태와 일치함을 확인했다. 문서의 복구 SQL 22개도 원본 정의와 대조했다.
- 서버 548건·Redis HTTP 7건·PMD/Checkstyle·JAR 빌드 통과. 마지막 로그 전용 보강 후 서버 전체 check·빌드를 다시 통과했다.
- V1–V29 범위에 실제 존재하는 기존 migration 28개 파일을 Git HEAD와 바이트 단위로 비교해 변경 없음을 확인했다. 새 V30만 추가했다.
- 원본 dev 작업 폴더는 clean이며 수정하지 않았다. 운영 DB나 데이터를 조회/변경하지 않았고 실제 운영 성능·DDL 소요 시간은 미검증이다.
