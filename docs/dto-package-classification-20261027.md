# DTO 패키지 분류 정리

- 작업일: 2026-10-27
- 브랜치: codex/permanent-notification-inbox-20261026
- 기준 HEAD: 787a441765008d8a2ab015441c97b2c6409c157a
- 범위: DTO 위치·package/import·필요한 접근성만 변경. API JSON·업무 동작·DB·설정 변경 없음.
- 커밋·push·merge·배포는 하지 않았다.

## 기준과 확인

AGENTS.md와 MVC/API 응답 의사결정의 DTO 분류 기준을 읽었다. 현재 작업 트리에는 DTO 관련 추가 스킬이 없으며, 원본 저장소의 .agents/skills에는 별도 심층 리뷰 스킬만 있었다. 이번 작업에 심층 리뷰 절차를 추가 적용하지 않았다.

API 입력은 dto/request, 실제 JSON 출력 및 그 중첩 모델은 dto/response, 내부 처리·조회 반환값은 dto/result로 분류한다. 접미사만으로 이동하지 않는다. 기존 분류가 맞으면 유지하고 필드를 복제하거나 변환 계층을 새로 만들지 않는다.

## 이동한 클래스

경로의 서버 접두어는 apps/server/src/main/java/bhoon/sugang_helper/다.

| 클래스 | 이전 위치 | 새 위치 | 실제 역할 |
| --- | --- | --- | --- |
| SeatNotificationDeliveryDispatch | seatalert/notification/service | seatalert/notification/dto/result | SettlementService.loadForDispatch가 Processor에 반환하는 내부 발송 정보 |
| SeatNotificationDeliveryFailureResult | seatalert/notification/service | seatalert/notification/dto/result | SettlementService.markFailure가 Processor에 반환하는 정산 결과 |
| SanitizedImage | common/util | common/dto/result | ImageUploadSanitizer가 LocalFileUploadService에 반환하는 이미지 처리 결과 |
| HealthCheckResponse | common/health | common/dto/response | HealthController.checkHealth가 실제 API body로 반환하는 응답 |

앞의 내부 record 3개는 다른 패키지에서 참조하기 위해 public으로 바꿨다. SeatNotificationDeliveryFailureResult.stale()도 호출부 접근에 필요한 public static으로 바꿨다. 필드·생성 데이터·메서드 동작은 유지했다. 참조 7개 파일은 import만 바뀌었다.

## 유지한 분류와 이유

dto 하위의 Java 타입은 총 125개다.

| 분류 | 수 | 이유·예시 |
| --- | ---: | --- |
| request | 26 | HTTP 입력·검증 모델 |
| response | 54 | 직접 응답 또는 응답 필드. Dashboard*Result, CrawlerAdminSnapshot, Timetable*Snapshot도 실제 응답에 포함되므로 유지 |
| result | 31 | User*Result, CourseSummary, LoadedFile, PrimaryTimetableSnapshot 등 내부 반환값 |
| command | 6 | HTTP 요청에서 변환한 서비스 입력 명령. API request와 역할이 다름 |
| criteria | 4 | 내부 조회 조건·리뷰 공유 키. CourseSearchCriteria, CourseSelectedSchedule, ReviewScopeKey, FeedbackSearchCondition |
| external | 2 | 대학 수집 데이터 매핑인 ParsedCourseDto/ParsedCourseScheduleDto |
| provider | 1 | 외부 알림 제공자 호출 대상 NotificationTarget |
| diagnostic | 1 | 수집 오류의 안전한 진단 요약 CrawlerFailureSummary |

AdminAuditMetadata/Data와 ContentSummary는 AdminActionLogResponse.metadata의 중첩 JSON이므로 response에 유지한다. 공통 ErrorResponse는 이미 분류된 common/response의 오류 계약을 유지한다. 이벤트는 event, 설정 record는 설정 패키지, Redis 세션의 SessionPrincipal은 기존 인증 상태 위치·직렬화 클래스명을 유지했다. 단일 구현 전용 private record도 외부 DTO로 끌어내지 않았다.

## 기존 미커밋 작업 보존

영구 알림함 구현은 여전히 설계만 승인된 상태다. 동결 32개 경로 중 30개는 바이트 단위로 동일하다. SeatNotificationOutboxProcessor와 그 테스트 두 파일은 새 결과 DTO import만 추가했다. 삭제되어 있던 NotificationHistoryQueryService, 미추적 NotificationHistoryService, V31 초안 및 모든 기존 내용은 그대로 보존했다. 미완성 기능을 완성·취소하거나 migration을 실행하지 않았다.

이번 변경을 기존 diff와 구분하기 위해 저장소 바깥 작업 폴더 dto-package-cleanup-20261027에 변경 전 Java 461개, before-state.json, changes.json, 패키지 보존 검사와 실행 로그를 보관했다. git diff의 전체 변경량을 이번 DTO 작업량으로 해석하면 안 된다.

## 검증 결과와 한계

- compileJava, checkstyleMain, pmdMain, bootJar 통과.
- HealthControllerTest 3개, LocalFileUploadServiceTest 13개, SeatNotificationOutboxProcessorTest 5개, DashboardSnapshotResponseContractTest 1개: 합계 22개 통과, 실패·오류·skip 0.
- 전체 테스트 컴파일은 이동 전·후 모두 WebPushDeliveryTest 283·300행의 assertThat(tx.execute(...)) 제네릭 추론 오류 4건으로 실패했다. 기존 테스트를 수정·삭제·비활성화하지 않았다.
- 전체 컴파일 차단과 별도로 관련 단위 테스트 4개 클래스만 저장소 바깥 임시 Gradle init script로 별도 출력 경로에 컴파일·실행했다. 프로젝트의 기본 test 설정·build.gradle은 수정하지 않았다. 최초 임시 실행의 NO-SOURCE는 테스트 통과로 계산하지 않고 실제 소스 실행 결과 22개만 집계했다.
- DTO 125개 package/path 일치, dto 바로 아래 미분류 타입 0, 이전 FQCN 참조 0. app.jar의 새 클래스 4개 존재 및 이전 위치 클래스 부재 확인.
- 이동 타입은 package·필요한 public 접근성 외 본문 동일, 참조 파일 7개는 import 외 동일. 클래스명·record 컴포넌트·Jackson/검증 설정을 바꾸지 않았다.
- UI·DB·외부 제공자·실발송은 변경·실행하지 않았다. 현재 작업 트리의 알림함 초안이 기능 완료 또는 배포 가능하다는 의미가 아니다.

실행 명령(작업 폴더 경로는 TASK로 축약):

    ./gradlew --offline --no-daemon compileJava compileTestJava
    ./gradlew --offline --no-daemon --init-script TASK/selected-unit-tests.gradle compileJava checkstyleMain pmdMain bootJar dtoCleanupUnitTest
    ./gradlew --offline --no-daemon --init-script TASK/selected-unit-tests.gradle dtoCleanupUnitTest compileTestJava --continue

첫 번째는 변경 전 기준선, 두 번째는 본문 빌드·정적 검사, 세 번째는 실제 관련 단위 테스트와 변경 후 전체 테스트 컴파일 결과다. 전체 테스트·전체 E2E 성공으로 보고하지 않는다.


## 후속 승인: DTO 변경만 선택적으로 로컬 커밋한다

2026-10-27 사용자가 DTO 변경만 로컬 커밋하도록 추가 승인했다. 위의 커밋 미실행 기록은 최초 정리 완료 시점의 상태다. 이번에는 DTO 이동·접근성·import와 이 작업 문서만 스테이징하고, 알림함 초안과 기존 WebPushDeliveryTest 오류는 포함하지 않는다. 혼합 파일 두 곳은 기존 HEAD에 DTO import만 추가한 내용으로 스테이징한다. push·merge·배포는 하지 않는다.
