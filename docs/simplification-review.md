# 후속 검토와 보류 범위

단일 Spring 앱을 유지한다. 아래는 읽기 전용 검토 결과이며 DB 기록·설정 삭제와 로그 전환을 구현하지 않았다. 서비스 분리·메시지 브로커·새 프레임워크를 제안하지 않는다.

## DB 상태와 진단 이력

| 대상 | 확인한 사용처 | 판단과 가장 작은 후속 변경 |
| --- | --- | --- |
| CrawlerRunFailure | CrawlerRunStateService.recordFailure에서 append. repository를 통한 운영 조회·API·UI·재시도·복구·중복 방지·정리 작업은 없음 | 진단 이력이다. 기존 CourseCrawlerService의 구조화 run_failed 로그가 runId·학기·단계·오류·소요시간을 남긴다. 승인 후 append만 제거할 수 있으나 SQL 사후 조사와 DB 보존 이력을 잃는다. 옛 테이블 DROP 없이 매핑/쓰기 제거부터 가능하다. |
| CrawlerStatus.lastSuccessAt | CrawlerRunStateService.findLastSuccessAt → CourseCrawlerService 신선도 검사 및 AdminService 상태/통계 | 재시작 후 유지되는 실제 동작 상태다. CourseCrawlCompletionService에서 강의·좌석·outbox와 같은 트랜잭션으로 성공 시각을 기록한다. 완전 로그 전환하면 재시작 직후 UNKNOWN/stale로 바뀐다. 유지하거나 그 동작 변경을 먼저 합의해야 한다. |
| CrawlerStatus 나머지 필드 | latestStatus·lastFailureAt/stage/type/message·updatedAt은 쓰기와 테스트 외 런타임 읽기 없음 | 진단 축소 후보다. latest_status/updated_at은 기존 V26 NOT NULL 열이므로 필드만 지우면 새 singleton 저장이 실패한다. 축소 승인 시 추가 migration과 재시작/실패 뒤 신선도 회귀가 필요하다. |
| AdminActionLog | AdminAuditService 기록/조회, GET /api/v1/admin/audit-logs. 문의 상태·답변·첨부 접근·DLQ 재전송에서 기록. 웹 조회 화면은 없음 | 업무 상태 자체는 아니지만 현재 업무 트랜잭션과 함께 확정되는 감사 이력이다. Grafana 로그로 바꾸려면 안전한 metadata/HMAC 요약을 유지하고 commit 이후에 성공 로그를 남겨 rollback을 성공처럼 기록하지 않아야 한다. 승인 후 조회 API/저장/180일 retention 제거를 한 단위로 검증한다. |

크롤러 중복 실행은 CourseCrawlerService의 프로세스 내 AtomicBoolean, 재시도는 JbnuCourseApiClient의 반복문이 담당한다. CrawlerStatus는 실행 선점이나 중단 지점 복구 기록이 아니다. 관리자 RUNNING/DEGRADED도 마지막 성공의 15분 신선도 기준이며 실제 실행 중 플래그와 다르다.

DB 제거와 로그 전환은 보존 계약도 다르다. 현재 logback은 14일/1GB, Loki는 30일 삭제이며 별도 영구 아카이빙 실행 여부는 확인하지 않았다. 예전 문서의 장기 압축 보관을 실제 보장으로 간주하지 않는다. 첨부 감사 기록은 서버가 접근을 승인하고 응답을 준비했다는 뜻이며 브라우저 다운로드 완료를 보장하지 않는다.

## 설정·보호 코드

| 분류 | 대상 | 근거 |
| --- | --- | --- |
| 제거 후보, 미실행 | SecurityConfig.passwordEncoder | 현재 주입·호출 없음. form/basic 로그인은 비활성이고 OAuth를 사용한다. 삭제는 별도 승인 뒤 인증 context 회귀로 확인한다. |
| 유지 | MdcTaskDecorator | 제공자 timeout 경계와 스케줄러에 요청/실행 추적 정보를 전파한다. @Async 부재만으로 미사용이라 판단하지 않는다. |
| 유지 | FirebaseConfig 및 Firebase 의존성 | PostConstruct의 FirebaseApp 초기화를 FcmNotificationSender가 FirebaseMessaging.getInstance()를 통해 사용한다. 설정 빈 직접 주입 여부만으로 삭제할 수 없다. |
| 유지 | Redis template/Lettuce, SecurityContextRepositoryConfig | 세션·토큰 철회와 장애 시 재전송/거절 정책에 실제 사용한다. |
| 유지 | SensitiveDataRedactor | fingerprint·maskEmail·failureCode·exceptionType·redactPath 다섯 함수가 로그·오류·발송에서 사용된다. 만능 비밀 탐지기는 아니지만 현재 보호 기능을 제거하면 원문 노출 가능성이 생긴다. |
| 추가 확인 | FlywayConfig | H2 repair와 실제 MySQL migrate의 실행 전략이 다르다. 단순 파일 수 감소를 위해 지우지 않는다. |
| 추가 확인 | applicationTaskExecutor 자동 구성 | 사용자 정의 빈은 없다. Spring MVC는 이름 기반 AsyncTaskExecutor를 조회하며 Boot는 기존 Executor에 따라 자동 구성이 달라진다. 비동기 컨트롤러는 발견하지 못했지만 전체 실제 배포 context의 생성 빈은 측정하지 않았다. 자동 구성 강제 제외를 추가하지 않는다. |

## 현재 추가 구현 권고

위 DB 진단 축소와 미사용 PasswordEncoder 정리가 작은 후보지만 삭제 승인을 받기 전에는 실행하지 않는다. 이미 단일 구현 전달 인터페이스·중복 D-day/일정 관리자 화면·공통 정상 래퍼를 정리했으므로 지금은 추가 추상화나 대규모 메서드 재편보다 현재 구조를 유지하는 편이 낫다.

기존에 확인한 HTTP timeout 후 하위 요청 잔존, 단일 scheduler 지연, 부분 성공 후 재시도의 중복 가능성은 알려진 한계다. 회로 차단 시 attempts 소진은 사용자가 그대로 두기로 했으므로 정책을 바꾸거나 재권고하지 않는다.

## 계획과 백로그

- 영구 알림함은 사용자별 항목·읽음 상태·권한·보존 규칙을 알림 발송 이력과 구분해 설계한다. 아직 엔티티·API·UI를 만들지 않는다.
- 공지는 발행/수정 중 어느 시점에 누구에게 전달할지, 기존 공지의 소급 알림 여부, 채널 수신 동의, 중복 방지 키·재시도·읽음/삭제·보존을 먼저 확정한다. 현재 outbox에 연결하지 않는다.
- 로깅 메시지 한국어화·레벨·위치·Grafana/Slack 가독성은 나중 할 일이다. 별도 관리자 로그 화면을 만들거나 이번 구현에 로그 정책 변경을 섞지 않는다.
