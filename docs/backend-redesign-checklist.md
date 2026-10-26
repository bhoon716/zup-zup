# 백엔드 전체 재설계 체크리스트

- 기준일: 2026-09-30
- 계획 연동 및 현행 지침 반영: 2026-10-01. [재설계 논의 계획](backend-redesign-plan.md)
- 대상: `apps/server`의 사용자 API, 관리자 기능, 백그라운드 작업, 저장소, 외부 연동, 운영 경계
- 목표: 인지 부채를 줄이고, 기능과 운영 근거가 확인되지 않은 기술 도입을 다시 평가한다.
- 기준선: 현재 라우트·스케줄러·도메인 테이블·Gradle 의존성을 코드에서 수집했다. 목록에 있다는 이유만으로 유지 필요성이 입증되는 것은 아니다.
- 결정 상태: 이 문서는 현황과 검토 항목을 기록한다. 기존 [진행 방식 제안](decisions/2026-10-01-backend-redesign-approach.md)은 철회했다. 목록은 유지 요구사항이 아니며, 단순 CRUD를 제외한 전체 기능의 필요한 동작·구조·기술을 사용자와 논의한 뒤 결정한다.

## 진행 상태

- [x] 백엔드 컨트롤러와 라우트 매핑 확인
- [x] 스케줄러, 이벤트 처리, 외부 연동 확인
- [x] Gradle 의존성과 Flyway 마이그레이션 범위 확인
- [ ] 각 API의 실제 사용자·웹 클라이언트 호출과 운영 사용 여부 대조
- [ ] 각 기능의 유지·수정·통합·제거 결정과 근거 기록
- [ ] 재설계 대상별 결정 문서 작성 및 적용
- [ ] 기능별로 필요한 단위·DB/보안/마이그레이션·실제 서버 E2E 검증 통과
- [ ] 코드 리뷰의 주요 지적 해결 및 기존 결정과 충돌 검토

표시 방법: `[ ]` 미검토, `[x]` 근거를 남기고 검토 완료. “미사용으로 보임”은 삭제 근거가 아니므로 호출자·운영 기록·데이터 이력을 확인한 뒤 판단한다.

## 모든 기능에 적용할 검토 항목

아래 항목은 각 기능의 재설계가 끝나기 전에 모두 확인한다. 적용되지 않는 항목은 근거와 함께 `해당 없음`으로 기록한다.

- [ ] 기능의 사용자·운영 목적과 보존할 동작을 확인한다. 웹 호출부, 운영 절차, 데이터로 사용 여부를 교차 확인한다.
- [ ] 결과를 `유지`, `수정`, `통합`, `제거` 중 하나로 정하고 근거를 기록한다.
- [ ] API 계약을 기록한다: 요청·응답, 필터·정렬·페이지네이션, 오류와 상태 코드, 인증·권한, 호환성.
- [ ] 데이터의 소유 기능, 기준 정보, 제약조건, 삭제·보존 기간과 개인정보를 확인한다.
- [ ] 동시 요청, 중복 요청, 트랜잭션 경계, 멱등성, 재시도 및 복구 동작을 확인한다.
- [ ] 부하와 지연의 근거를 확인한다: 호출량, 데이터 규모, 쿼리, 인덱스, 외부 호출 제한.
- [ ] 장애를 관측하고 대응할 수 있는 로그·지표·알림이 필요한지 결정한다. 로그와 알림에서 비밀·개인정보가 노출되지 않는지 확인한다.
- [ ] 도입 기술마다 실제 사용 지점, 해결하려는 문제, 운영·개발 비용, 단순한 대안, 선택 근거를 기록한다.
- [ ] API·데이터·보안·의존성·인프라에 영향을 주는 선택은 구현 전에 ADR로 기록하고 결정 목록에 등록한다.
- [ ] 기존 데이터와 웹 클라이언트를 고려한 전환·롤백 방법을 기록한다.
- [ ] 변경 위험에 맞는 테스트를 작성하고 실행한다. 단위·필요한 DB/Redis/보안 경계·마이그레이션·실제 서버 E2E에서 검증하는 보장을 구분한다.
- [ ] 코드 리뷰 후 주요 지적을 해결하고, 기존 결정과 충돌하는지 확인한다.

## 기능 인벤토리와 재설계 체크

아래 API 목록은 컨트롤러 매핑과 보안 설정을 기준으로 한다. OAuth2·Actuator처럼 프레임워크 또는 운영 설정이 제공하는 경로도 함께 적었다.

### 인증·계정·사용자 설정

근거: [SecurityConfig](../apps/server/src/main/java/bhoon/sugang_helper/bootstrap/SecurityConfig.java), [AuthController](../apps/server/src/main/java/bhoon/sugang_helper/identity/auth/presentation/AuthController.java), [UserController](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/presentation/UserController.java), [UserDeviceController](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/presentation/UserDeviceController.java), [application.yml](../apps/server/src/main/resources/application.yml)

- Google OAuth2 로그인: `GET /oauth2/authorization/google`, `GET /login/oauth2/code/google` (Spring Security 기본 경로)
- 토큰·세션: `POST /api/auth/refresh`, `POST /api/auth/logout`
- 가입·이메일: `POST /api/v1/users/onboard`, `POST /api/v1/users/email/code`, `POST /api/v1/users/email/verify`
- 프로필·설정·탈퇴: `GET|PATCH /api/v1/users/me`, `PATCH /api/v1/users/settings`, `DELETE /api/v1/users/me`
- Discord 계정 연동: `GET /api/v1/users/discord/authorize`, `GET /api/v1/users/discord/callback`, `DELETE /api/v1/users/me/discord`
- 푸시 기기 등록: `GET|POST|DELETE /api/v1/users/devices`, `DELETE /api/v1/users/devices/{id}`, `DELETE /api/v1/users/devices/token/{token}`
- 테스트 알림 진입점: `POST /api/v1/users/notifications/test`, `POST /api/v1/notifications/test`

- [ ] OAuth 공급자·식별자 연결·기존 계정 연결·탈퇴 데이터 처리의 규칙과 실패 흐름을 정리한다.
- [ ] JWT 만료·갱신·폐기와 Redis 상태, Redis HTTP 세션의 각각의 필요성과 수명 주기를 검토한다.
- [ ] 이메일 코드 생성·저장·만료·재요청 제한·검증 완료 처리를 확인한다.
- [ ] 프로필·알림 설정·Discord 연동·기기 소유권과 계정 삭제/탈퇴 영향을 확인한다.
- [ ] 두 사용자 테스트 알림 경로의 목적·권한·중복 여부를 확인한다.
- [ ] 리다이렉트·쿠키·CORS·CSRF·토큰 저장 위치·로그 마스킹을 함께 검토한다.

### 강의 탐색·강의 카탈로그

근거: [CourseController](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/presentation/CourseController.java), [CourseSearchTargetController](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/presentation/CourseSearchTargetController.java), [CourseSearchReadRepository](../apps/server/src/main/java/bhoon/sugang_helper/readmodel/course/infra/CourseSearchReadRepository.java)

- `POST /api/v1/courses/search`
- `GET /api/v1/courses/{courseKey}`
- `GET /api/v1/courses/{courseKey}/history`
- `GET /api/v1/courses/search-default-semester`

- [ ] 검색 조건·정렬·페이지네이션·기본 학기 결정 규칙을 클라이언트 호출과 대조한다.
- [ ] 검색 읽기 모델과 JPA/QueryDSL 쿼리의 책임·복잡도·성능 근거를 검토한다.
- [ ] 강의 상세·시간표·여석 이력의 기준 데이터, 누락·갱신 시점, 공개 범위를 확인한다.
- [ ] 강의 식별자와 문자열 정렬·collation 제약이 필요한 이유를 현재 데이터로 확인한다.

### 학사 데이터 수집·크롤러 운영

근거: [AdminCourseController](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/presentation/AdminCourseController.java), [CourseScheduler](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/application/CourseScheduler.java), [CourseCrawlerService](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/application/CourseCrawlerService.java), [JbnuCourseApiClient](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/infra/JbnuCourseApiClient.java), [CourseCatalogSynchronizationService](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/application/CourseCatalogSynchronizationService.java)

- `GET|PUT /api/v1/admin/courses/crawl-target`
- `POST /api/v1/admin/courses/crawl`, `POST /api/v1/admin/courses/crawl/target`
- 예약 실행, 시작 시 실행 옵션, 데이터 최신성 확인, JBNU 외부 API 조회·파싱·검증·동기화
- 실행 상태와 단계별 실패 저장, 크롤러 실패 시 Slack 알림

- [ ] 크롤링 대상·학기·분류별 조회 방식과 JBNU API 사용 조건을 확인한다.
- [ ] 주기·시작 시 실행·수동 실행·중복 실행 방지 및 다중 인스턴스 동작을 정리한다.
- [ ] 연결·응답 시간 초과, 응답 크기 제한, 재시도·요청 간격, 파싱 실패 정책의 근거를 확인한다.
- [ ] 부분 응답 또는 잘못된 카탈로그가 저장되지 않는 조건과 원자적 동기화 경계를 확인한다.
- [ ] 크롤러 상태·실패 이력·오래된 상태 판정·알림의 운영상 필요성과 보존 기간을 확인한다.
- [ ] 소스 사이트의 변경·장애를 감지하고 이전 정상 데이터를 유지·복구하는 방법을 정의한다.

### 강의 리뷰·반응

근거: [CourseReviewController](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/presentation/CourseReviewController.java), [CourseEmojiReviewController](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/presentation/CourseEmojiReviewController.java)

- 리뷰: `POST|GET /api/v1/courses/{courseKey}/reviews`, `PUT|DELETE /api/v1/courses/reviews/{reviewId}`
- 리뷰 반응: `POST /api/v1/courses/reviews/{reviewId}/reaction`
- 이모지 반응: `GET /api/v1/courses/{courseKey}/emojis`, `POST /api/v1/courses/{courseKey}/emojis/toggle`

- [ ] 리뷰 작성·수정·삭제와 별점/평균·집계 반영 규칙을 확인한다.
- [ ] 사용자별 중복 제한, 비공개/삭제 상태, 페이지네이션, 악용 방지와 관리자 조치 필요성을 정리한다.
- [ ] 리뷰 반응과 이모지 반응이 각각 제공하는 사용자 가치를 확인하고 중복 모델 여부를 검토한다.
- [ ] 사용자 탈퇴·강의 삭제 시 리뷰와 반응 보존·익명화 규칙을 확인한다.

### 시간표·찜 목록

근거: [TimetableController](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/presentation/TimetableController.java), [WishlistController](../apps/server/src/main/java/bhoon/sugang_helper/planning/wishlist/presentation/WishlistController.java), [TimetableService](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/application/TimetableService.java)

- 시간표: `POST|GET /api/v1/timetables`, `GET /api/v1/timetables/primary`, `GET /api/v1/timetables/{timetableId}`
- 시간표 강의: `POST /api/v1/timetables/{timetableId}/courses`, `DELETE /api/v1/timetables/{timetableId}/courses/{courseKey}`
- 사용자 일정: `POST /api/v1/timetables/{timetableId}/custom-schedules`, `DELETE /api/v1/timetables/{timetableId}/custom-schedules/{scheduleId}`
- 대표 시간표·삭제: `PATCH /api/v1/timetables/{timetableId}/primary`, `DELETE /api/v1/timetables/{timetableId}`
- 찜: `POST /api/v1/wishlist/{courseKey}`, `GET /api/v1/wishlist`

- [ ] 사용자 가입 이벤트에서 기본 시간표를 자동 생성하는 흐름과 중복 이벤트 동작을 확인한다.
- [ ] 시간표 소유권, 대표 시간표 유일성, 시간표/일정 간 참조와 삭제 규칙을 확인한다.
- [ ] 강의 시간 충돌·중복 등록·강의 카탈로그 변경 시 현재 사용자 기대 동작을 확인한다.
- [ ] 찜 토글의 동시 요청·멱등성·목록 정렬 규칙과 구독 기능과의 차이를 확인한다.
- [ ] 탈퇴 시 시간표·개인 일정·찜 데이터의 보존·삭제 정책을 확인한다.

### 여석 구독·알림 이력·발송

근거: [SubscriptionController](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/subscription/presentation/SubscriptionController.java), [NotificationController](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/presentation/NotificationController.java), [SeatNotificationOutboxWorker](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/SeatNotificationOutboxWorker.java), [NotificationService](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/NotificationService.java)

- 구독: `POST|GET /api/v1/subscriptions`, `DELETE /api/v1/subscriptions/{id}`, `DELETE /api/v1/subscriptions`
- 사용자 알림 이력: `GET /api/v1/notifications/history`
- 현재 코드의 채널 발송: 이메일, Discord, Web Push, FCM. 재설계에서는 이메일·Discord·Web Push를 유지하고 FCM을 제거하기로 결정했다. [채널 정책 결정](decisions/2026-10-01-notification-channel-policy.md)
- 내부 처리: 좌석 개방 이벤트, DB outbox, 채널별 delivery, 재시도·DLQ·보관 정리

- [ ] 알림을 보내는 조건, 구독 상한, 강의별 중복 구독 및 구독 취소 경합을 확인한다.
- [ ] 한 좌석 개방 사건에서 수신자·채널·알림 이력이 산출되는 규칙을 정의한다.
- [ ] outbox 이벤트 기록·claim/lease·동시 worker·중복 방지·재시도·DLQ 전환의 보장을 확인한다.
- [ ] 관리자 재처리의 안전 조건, DLQ 보존/삭제 기간, 사용자의 알림 이력 보존을 정한다.
- [ ] 채널 사용 여부와 사용자 동의·설정, 유효하지 않은 기기 제거·Discord 연동 해제의 효과를 확인한다.
- [x] 선택한 채널 각각에 전달하고 채널별 발송 결과를 구분하는 방향을 결정한다. 구체적인 재시도 정책은 미정이다.
- [x] PWA 전환에 따라 FCM 제거와 이메일·Discord·Web Push 유지를 결정한다.
- [x] 재발송 우선·중복 알림 허용·원래 감지 시각 표시와 중복 방지 목적의 MQ 보류를 결정한다. [결정 기록](decisions/2026-10-01-notification-duplicate-and-mq-policy.md)
- [x] at-least-once 전달 목표와 외부 서비스의 수락·처리 응답을 완료 기준으로 결정한다. [결정 기록](decisions/2026-10-01-notification-at-least-once-delivery.md)
- [x] 늦게라도 발생 사실을 전달하며 지연만을 이유로 미발송 작업을 취소·만료·삭제하지 않는 방향을 결정한다. [결정 기록](decisions/2026-10-01-delayed-seat-alert-delivery.md)
- [x] 구독 해제 요청 처리에서 대기 알림을 취소하고, 발송·재시도 직전 구독 상태 재조회는 추가하지 않는 방향을 결정한다.
- [ ] 구독 해제 시 취소할 작업 상태와 worker 경합·재구독·발송 작업 생성 시점을 설계하고 취소 처리를 구현한다.
- [ ] 발송 생략·설정 누락과 실제 성공을 구분하고, 미전달 작업의 재시도·보존·복구 정책을 정한다.
- [x] 이메일·Discord·Web Push의 공식 멱등성 계약과 현재 구현을 조사한다. [조사 자료](backend-notification-idempotency-research.md)
- [ ] 최초 감지 시각의 저장·보존·메시지 표시와 실제 상태 차이 안내를 구현한다.
- [ ] 서버·웹 FCM/Firebase 구현·설정·의존성·배포/CI 연결을 제거하고 기존 FCM 데이터의 전환 방법을 정한다.
- [ ] 이메일·Discord·Web Push 공급자별 시간 초과·회로 차단·실패 분류·복구 기준을 검토한다.
- [ ] outbox가 요구하는 전달 보장과 현재 규모를 증거로 남기고, 더 단순한 대안과 비용을 비교한다.

### 공지·일정·D-day·대시보드

근거: [AnnouncementController](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/presentation/AnnouncementController.java), [AdminAnnouncementController](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/presentation/AdminAnnouncementController.java), [ScheduleController](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/presentation/ScheduleController.java), [DdaySettingController](../apps/server/src/main/java/bhoon/sugang_helper/campus/dday/presentation/DdaySettingController.java), [DashboardController](../apps/server/src/main/java/bhoon/sugang_helper/dashboard/presentation/DashboardController.java)

- 공지 조회: `GET /api/v1/announcements`, `GET /api/v1/announcements/{id}`
- 공지 관리자 CRUD: `GET|POST /api/v1/admin/announcements`, `PUT|DELETE /api/v1/admin/announcements/{id}`
- 일정 조회·관리: `GET /api/v1/schedules`; `GET|POST /api/v1/admin/schedules`, `PUT|DELETE /api/v1/admin/schedules/{id}`
- D-day 조회·관리: `GET /api/v1/ddays/active`; `GET|POST /api/v1/admin/ddays`, `PUT|DELETE /api/v1/admin/ddays/{id}`
- 사용자 대시보드: `GET /api/v1/dashboard`; 관리자 대시보드: `GET /api/v1/admin/dashboard`

- [ ] 공지·일정·D-day의 공개·예약·정렬·시간대 규칙과 관리 변경 이력을 확인한다.
- [ ] 활성 D-day 선택 규칙과 여러 활성 항목이 가능한지 확인한다.
- [ ] 대시보드가 합치는 데이터와 최신성·실패 허용 범위·응답 지연 목표를 확인한다.
- [ ] 관리자 트래픽·최근 로그 집계의 사용자 가치와 개인정보 최소화를 확인한다.
- [ ] 읽기 API의 공개 범위가 실제 제품 요구와 일치하는지 대조한다.

### 사용자 문의·첨부 파일·관리자 답변

근거: [FeedbackController](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/presentation/FeedbackController.java), [AdminFeedbackController](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/presentation/AdminFeedbackController.java), [LocalFileUploadService](../apps/server/src/main/java/bhoon/sugang_helper/common/util/LocalFileUploadService.java), [application.yml](../apps/server/src/main/resources/application.yml)

- 사용자: `POST /api/v1/feedbacks` (multipart), `GET /api/v1/feedbacks/me`, `GET /api/v1/feedbacks/{feedbackId}`, `GET /api/v1/feedbacks/{feedbackId}/attachments/{attachmentId}`, `DELETE /api/v1/feedbacks/{feedbackId}`
- 관리자: `GET /api/v1/admin/feedbacks`, `GET /api/v1/admin/feedbacks/{feedbackId}`, `POST /api/v1/admin/feedbacks/{feedbackId}/attachments/{attachmentId}/download`
- 상태·답변: `PATCH /api/v1/admin/feedbacks/{feedbackId}/status`, `POST /api/v1/admin/feedbacks/{feedbackId}/reply`, `PATCH /api/v1/admin/feedbacks/reply/{replyId}`, `DELETE /api/v1/admin/feedbacks/reply/{replyId}`
- 내부 처리: 파일 형식·이미지 크기 검증, 업로드 용량/요청 제한, 사용자·IP별 rate limit 및 일일 quota, 참조되지 않는 파일 정리

- [ ] 문의 상태 전이·답변 권한·사용자 열람 범위와 삭제 동작을 정의한다.
- [ ] 파일의 경로 안전성, 실제 파일 유형, 악성 파일 위험, 크기·이미지 처리 한계를 검토한다.
- [ ] 파일과 DB의 원자성, 고아 파일 정리, 백업·복구, 다중 인스턴스 공유 가능성을 확인한다.
- [ ] rate limit 저장소·초기화 단위·우회 가능성·정책 근거를 확인한다.
- [ ] 첨부 파일·문의 내용의 보유 기간, 사용자 삭제 요청, 백업 내 삭제 반영을 정한다.

### 관리자 도구·감사 기록

근거: [AdminController](../apps/server/src/main/java/bhoon/sugang_helper/admin/presentation/AdminController.java), [AdminActionLogController](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/presentation/AdminActionLogController.java), [AdminNotificationDeliveryController](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/presentation/AdminNotificationDeliveryController.java), [AdminAuditLogRetentionScheduler](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/application/AdminAuditLogRetentionScheduler.java)

- 관리자 대시보드: `GET /api/v1/admin/dashboard`
- 관리자 테스트 발송: `POST /api/v1/admin/notifications/test`
- 감사 기록: `GET /api/v1/admin/audit-logs`
- 알림 실패 관리: `GET /api/v1/admin/notification-deliveries/dlq`, `GET /api/v1/admin/notification-deliveries/{deliveryId}`, `POST /api/v1/admin/notification-deliveries/{deliveryId}/replay`

- [ ] 관리자 권한 부여·회수·계정 보호와 모든 관리자 경로의 권한 검사를 확인한다.
- [ ] 실제로 감사할 작업·기록 주체·대상·변경 요약·민감 정보 제거·변조 방지 필요성을 정한다.
- [ ] 감사 로그 fingerprint와 보존 기간·삭제 기준·조회 권한을 확인한다.
- [ ] DLQ 재발행 및 테스트 발송의 오발송 방지·속도 제한·감사 기록을 확인한다.
- [ ] 관리자 API의 페이지네이션·대량 조회 한도·운영 사용자 경험을 확인한다.

### 공통 HTTP·오류·보안·운영 표면

근거: [GlobalExceptionHandler](../apps/server/src/main/java/bhoon/sugang_helper/common/web/GlobalExceptionHandler.java), [PageableGuard](../apps/server/src/main/java/bhoon/sugang_helper/common/web/PageableGuard.java), [SecurityConfig](../apps/server/src/main/java/bhoon/sugang_helper/bootstrap/SecurityConfig.java), [application-prod.yml](../apps/server/src/main/resources/application-prod.yml)

- 공개 상태 확인: `GET /health`
- 운영 관리 포트: Actuator health/readiness 및 Prometheus endpoint (운영 프로필의 포트 `8081`)
- 공통 기능: API 응답·예외 매핑, CORS, JWT 필터, role 검사, 페이지 요청 제한, 로그 마스킹, graceful shutdown, 지표

- [ ] 익명 허용 경로·로그인 필요 경로·관리자 경로와 데이터 소유자 검사를 라우트별로 확정한다.
- [ ] 관리 포트가 애플리케이션 외부에서 접근되지 않는지 인프라 방화벽·프록시 설정과 함께 확인한다.
- [ ] CSRF·쿠키 자격 증명·CORS·OAuth 리다이렉트·세션 고정과 로그아웃/탈퇴 폐기를 검토한다.
- [ ] 오류 응답 계약과 내부 오류·외부 공급자 오류의 사용자 노출 범위를 정한다.
- [ ] readiness가 DB/Redis를 검사하는 목적, 배포·재시작 정책과의 연결을 확인한다.
- [ ] 지표·로그가 실제 장애 탐지와 해결에 쓰이는지 확인하고, 과도하거나 중복된 관측 도구를 정리한다.

## 백그라운드 작업과 이벤트 흐름

근거: [SugangHelperApplication](../apps/server/src/main/java/bhoon/sugang_helper/SugangHelperApplication.java), [FeedbackAttachmentCleanupScheduler](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/application/FeedbackAttachmentCleanupScheduler.java), [SeatNotificationOutboxWorker](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/SeatNotificationOutboxWorker.java), [NotificationDeliveryRetentionScheduler](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/NotificationDeliveryRetentionScheduler.java)

- [ ] 크롤러 예약·기동 시 실행·최신성 감시 작업의 주기·소유 인스턴스·중복 실행 정책을 검토한다.
- [ ] 알림 outbox polling·worker pool·queue·batch·lease·attempt 한도의 실제 트래픽 근거와 장애 시 backlog 동작을 확인한다.
- [ ] `UserRegisteredEvent` 처리에 의한 기본 시간표 생성의 이벤트 전달 보장과 재처리 동작을 확인한다.
- [ ] outbox 생성에 사용되는 트랜잭션 이벤트(`BEFORE_COMMIT`)와 발송 작업 사이의 원자성·경합을 검토한다.
- [ ] 감사 로그·알림 DLQ·고아 첨부 파일 정리 작업의 시간대·삭제량·실패 관측·재실행 안전성을 확인한다.
- [ ] `@Async` 실행기 설정과 비동기 예외·종료 시 대기·큐 포화 동작을 확인한다.

## 데이터·마이그레이션 범위

근거: [Flyway migrations](../apps/server/src/main/resources/db/migration/), [build.gradle](../apps/server/build.gradle)

| 기능 영역 | 현재 스키마에서 확인한 객체 |
| --- | --- |
| 사용자·인증 연계 | `users` |
| 강의·수집 | `courses`, `course_schedules`, `course_seat_histories`, `crawler_settings`, `crawler_status`, `crawler_run_failures` |
| 리뷰 | `course_reviews`, `course_review_reactions`, `course_emoji_reviews` |
| 시간표·찜 | `timetables`, `timetable_entries`, `custom_schedules`, `custom_schedule_times`, `wishlists` |
| 알림 | `subscriptions`, `user_devices`, `notification_histories`, `seat_notification_outbox`, `seat_notification_deliveries` |
| 캠퍼스 정보 | `announcements`, `schedules`, `dday_settings` |
| 문의·관리자 기록 | `feedbacks`, `feedback_attachments`, `feedback_replies`, `admin_action_logs` |
| 검토 필요 | V11에서 생성하는 `BATCH_*` 메타데이터 테이블 |

- [ ] 테이블별 생성·읽기·수정·삭제 책임과 실제 운영 데이터 유무를 확인한다.
- [ ] nullable/unique/foreign key/index/collation/soft delete가 표현하는 실제 도메인 규칙을 확인한다.
- [ ] 데이터 정합성·중복 정리·기존 데이터 이동·롤백 절차를 기능별로 설계한다.
- [ ] `V11__add_spring_batch_tables.sql`은 Spring Batch 5.x 메타데이터 테이블을 만들지만, 현재 `build.gradle`과 Java 소스 검색에서 Spring Batch 의존성·Job 코드는 확인되지 않았다. 과거 사용·운영 데이터·계획을 확인하고 보존 또는 제거 여부를 별도 결정한다.
- [ ] 프로덕션에서 Flyway를 앱 시작과 분리한 이유와 배포 migrator 경로를 확인한다.
- [ ] 테이블·컬럼 전환 시 구버전 앱과 신규 앱이 함께 동작해야 하는 기간 및 복구 수단을 검증한다.

## 기술·의존성 정당성 검토

근거: [build.gradle](../apps/server/build.gradle), [application.yml](../apps/server/src/main/resources/application.yml), [application-prod.yml](../apps/server/src/main/resources/application-prod.yml)

아래 항목은 현재 선언 또는 설정된 기술이며, 재설계 시 실제 호출 위치와 대안을 다시 검토한다. 보안 패치 제약은 CVE 근거를 먼저 확인한 뒤 변경한다.

| 기술 영역 | 코드에서 확인한 역할 또는 위치 | 재설계 체크 |
| --- | --- | --- |
| Java 21, Spring Boot, Spring MVC, Validation | 서버 런타임과 HTTP/API 입력 검증 | [ ] 런타임 지원·프레임워크 전체 기능 대비 단순화 여지를 확인한다. |
| Spring Security, OAuth2 Client, JJWT | Google 로그인, 권한·요청 인증, access/refresh token | [ ] 현재 로그인 방식·세션과 토큰 동시 사용 근거 및 보안 요구를 확인한다. |
| Spring Session Redis, Spring Data Redis | HTTP 세션과 인증 폐기·이메일 검증·rate limit 등의 Redis 사용 | [ ] Redis 키 목록·TTL·장애 시 동작·필수성과 한 저장소 장애 영향도를 확인한다. |
| Spring Data JPA, QueryDSL | 관계 데이터 접근과 `CourseSearchReadRepository` 동적 검색 | [ ] QueryDSL이 해결하는 실제 검색 요구와 JPQL/SQL 대안을 비교한다. |
| Flyway, MySQL, H2 | 운영 스키마 버전 관리와 로컬/테스트 데이터베이스 | [ ] H2와 MySQL 차이, migration 검증 경로, 배포 순서의 비용을 확인한다. |
| AWS Spring Cloud SES, Spring Mail | 이메일 발송 구성 | [ ] SES와 Mail 추상화의 실제 연결·중복 계층·로컬 대체 필요성을 확인한다. |
| Firebase Admin, Web Push, BouncyCastle, Discord API | FCM, 브라우저 push, Discord OAuth/알림 | [ ] 채널별 실제 이용·사용자 설정·운영 비용과 미사용 채널 제거 조건을 확인한다. |
| Jsoup | JBNU 수집 응답 파싱 | [ ] 파서가 의존하는 문서 구조와 크기·보안 제약을 확인한다. |
| Apache Tika, 로컬 파일 업로드 처리 | 문의 첨부 형식 검사·파일/이미지 제약 | [ ] 실제 MIME 탐지 범위와 파일 저장·검증 대안을 검토한다. |
| Micrometer Prometheus, Actuator | 앱 지표·health/readiness endpoint | [ ] 대시보드·알림에서 쓰는 지표와 외부 가시성 경계를 확인한다. |
| Slack webhook | 크롤러 실패와 서버 오류 알림 | [ ] 알림을 받는 사람·대응 절차·중복 억제·장애 시 대안을 확인한다. |
| Springdoc OpenAPI | 개발 환경 API 문서 경로와 관리자 제한 | [ ] 현재 사용 여부·운영 노출 정책·계약 문서 생성에 필요한지 확인한다. |
| Apache Commons Lang | 직접 의존성 및 Springdoc의 제외 후 명시 버전 고정 | [ ] 직접 호출·전이 의존성·버전 고정 사유를 확인한다. |
| Lombok | 컴파일 시 애너테이션 처리 | [ ] 팀의 사용 범위·IDE/빌드 복잡도와 수동 코드 대안을 비교한다. |
| Checkstyle, PMD | Gradle 정적 검사 플러그인과 별도 설정 파일 | [ ] 실제 위반 방지 효과·규칙 중복·유지 비용을 확인한다. |
| Spring Boot Test, Security Test, H2, Testcontainers, dotenv | 테스트 의존성. Testcontainers는 MySQL 마이그레이션 검증 태스크에 선언됨 | [ ] 단위·필요한 경계/마이그레이션·E2E 검증의 역할과 환경 차이·중복·비용을 확인한다. |
| jose4j, async-http-client, grpc-netty-shaded 버전 제약 | `build.gradle`의 보안 취약점 수정 사유가 적힌 제약 | [ ] 전이 경로와 취약점 수정 버전을 확인하고, 임의로 제약을 풀지 않는다. |
| Spring Batch `BATCH_*` 스키마 | V11 마이그레이션에서만 확인 | [ ] 실제 프레임워크 사용·데이터·운영 필요가 남아 있는지 확인한다. |

기술 도입 평가 기록에는 다음을 남긴다.

- [ ] 사용하는 기능과 호출 위치
- [ ] 해결하려는 문제와 유지해야 하는 사용자 동작
- [ ] 공식 문서·운영 기록·측정 결과 등 근거
- [ ] 더 작은 대안 및 전환 비용
- [ ] 업데이트·장애 대응·보안 패치 비용
- [ ] 선택 결과, 검증 조건, 재검토 조건을 포함한 ADR

## 재설계 완료 기준

기능 단위로 아래 조건을 충족하면 해당 기능의 재설계를 완료로 표시한다.

- [ ] 웹 호출·운영 사용·데이터 흐름에 근거해 기능 범위를 확정했다.
- [ ] 유지·수정·통합·제거와 선택 이유를 문서에 남겼다.
- [ ] API·권한·데이터·실패·성능·관측 계약을 정리했다.
- [ ] 새 기술 도입 및 기존 기술 유지에 근거가 있고, 주요 선택은 ADR에 기록했다.
- [ ] 필요한 스키마·클라이언트 전환·배포 순서·롤백 절차를 검토했다.
- [ ] 기능에 필요한 테스트와 실제 서버를 통과하는 핵심 흐름 검증이 통과했다.
- [ ] 코드 리뷰의 주요 지적을 해결하고, 남은 위험을 기록했다.
- [ ] 기존 결정과 충돌하지 않는지 확인했다.

## 추가 검토 기준 — 2026-10-13: 클린 아키텍처

기준: [클린 아키텍처 적용과 기존 의사결정 점검](decisions/2026-10-13-clean-architecture-policy.md). 앞선 목록에서 기술 통합을 검토하더라도 업무 코어의 외부 의존을 차단하는 경계를 우선한다.

- [x] 기존 의사결정 문서 27개의 아키텍처 양립 여부를 점검하고 변경·배치 보완·유지를 구분해 기록했다. 전체 소스의 전환 완료를 뜻하지 않는다.
- [ ] 도메인·유스케이스에서 Spring·JPA·Servlet·Redis·메일·HTTP·로깅 기술 의존을 제거한다.
- [ ] 순수 저장소·검색·발송·파일 등 필요한 출력 계약과 바깥 어댑터를 연결한다.
- [ ] 도메인과 JPA 영속성 모델을 구분하고 기존 쿼리·잠금·DB 제약·성능 영향을 확인한다.
- [ ] 경계 데이터에서 Spring 페이지 타입·JPA 엔티티·HTTP 업로드·외부 JSON 표현을 분리한다.
- [ ] 같은 DB의 원자적 저장 범위를 바깥 트랜잭션 실행으로 보존하고 외부 호출 경계를 유지한다.
- [ ] 업무 오류와 HTTP 상태·오류 응답 매핑을 분리한다.
- [ ] 기능 간 조회와 동기 직접 호출의 의존 방향을 확인하고 불필요한 일괄 인터페이스 추가를 피한다.

테스트 실행 범위는 사용자 지침의 단위 테스트·최종 E2E만 적용한다. 이전 목록의 DB·보안·마이그레이션 확인 항목을 별도의 통합 테스트 도입 요구로 해석하지 않는다. 이번 문서 정정에서 테스트를 추가하거나 실행하지 않았다.

## 변경 기준 — 2026-10-13: MVC 폴더 정리 후 기능 전환

최신 기준은 [기능별 MVC 레이어드 구조](decisions/2026-10-13-mvc-layered-architecture-policy.md)다. 위 클린 아키텍처 항목의 순수 모델·포트·어댑터·바깥 트랜잭션 강제 분리는 적용하지 않는다. 사용자 동작과 데이터 보존에 관한 기존 기능 결정은 유지한다.

- [x] 직전 미커밋 구현을 취소하고 `7149c32b`의 기존 동작을 기준으로 삼았다.
- [x] 기능별 `entity/`, `controller/`, `repository/`, `service/`와 필요한 역할 폴더로 메인 소스 342개 및 기존 테스트 소스 95개를 옮겼다. [이동 목록](backend-mvc-package-moves.md)을 참고한다.
- [x] 메인 소스 380개와 기존 테스트 소스 122개의 본문이 package·import·타입 경로 변경을 제외하고 유지되는지 확인했다.
- [x] 메인·기존 테스트 소스를 컴파일했다. 테스트 추가·실행은 하지 않았다.
- [ ] 폴더 정리 이후 수집 → 강의 갱신 → outbox 기록부터 합의한 기능 설계를 구현한다. 이번 폴더 이동을 기능 재설계 완료로 표시하지 않는다.

API·DB 스키마·업무 로직·트랜잭션·이벤트 흐름 변경은 후속 작업이다. 기존 인터페이스 통합도 실제 기능을 변경할 때 판단한다.

### 추가 반영: DTO 세분화·빈 폴더·Swagger

- [x] 기능별 `dto/request/`와 `dto/response/`로 요청·응답 DTO를 구분했다. 내부 공유 데이터는 `dto/`에 유지한다.
- [x] 이동 뒤 남은 빈 프로젝트 폴더를 정리했다.
- [x] 사용자 추가 요청에 따라 Swagger 문서 애너테이션·의존성·설정·문서 접근 경로와 관련 기존 테스트를 정리했다. 업무 API의 실제 매핑과 검증은 유지한다.

위 본문 보존 확인 이후 추가된 변경은 타입 경로 정리와 Swagger 제거다. 검증 기준에서도 Swagger 문서 애너테이션 및 전용 보안 경로·기존 테스트 제거를 허용된 변경으로 구분한다.

### 추가 반영: 중복 저장소 통합

- [x] 사용자 요청에 따라 중복 JPA 저장소 25쌍을 `XRepository` 하나로 통합했다.
- [x] 강의 검색·관리자 문의 조회의 단일 구현과 중간 저장소 계약을 통합했다.
- [x] 기존 쿼리·잠금·수정 애너테이션과 계약에만 있던 메서드를 보존하고 사용하지 않는 `RepositoryContract`를 제거했다.

이번 추가 변경은 저장소 타입 통합과 관련 참조 정리다. 업무 정책·API·테이블 변경은 포함하지 않는다. [저장소 통합 결정](decisions/2026-10-13-repository-interface-policy.md)을 최신 기준으로 적용한다.
