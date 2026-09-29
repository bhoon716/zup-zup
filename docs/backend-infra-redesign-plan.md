# 백엔드 코드·인프라·배포 재설계 계획

> 상태: **제안 / 설계 착수 문서** · 기준: 2026-09-27, 저장소 `9e81c0a0` · 범위: `apps/server` 코드 전반, `infra`, 서버 CI/CD, 운영 문서 및 API 계약. **프론트엔드 구현은 변경하지 않는다.**
>
> 이 문서는 작업 순서와 선택 기준을 정의한다. 목표 구조와 수치는 아래 결정 게이트를 통과하기 전까지 확정 사양이 아니다. 저장소 정적 사실과 운영 현실은 구분한다.

## 읽는 순서와 증거 규칙

1. [현재 시스템 인벤토리](current-system.md)의 기능/API/데이터/운영 경계를 출발점으로 삼는다. 해당 문서의 `Confidence`와 `Unknowns`를 함께 읽는다.
2. 이 문서의 **파트 A**에서 필요한 품질과 설계 결정을 확정하고, **파트 B~F**의 작업을 의존성 순서로 실행한다. **파트 G**는 전환·복구 기준이다.
3. 현재 상태를 주장할 때는 현 HEAD의 코드·설정·테스트를 우선한다. 이슈와 ADR은 당시의 의도와 실패 이력을 보여주지만 현재 배포의 증거는 아니다. 운영 수치·설정·백업 상태는 운영 호스트에서 별도 확인해야 한다.

근거의 중심은 `apps/server/build.gradle`, `apps/server/src/main/java`, `apps/server/src/main/resources/application-prod.yml`, `apps/server/src/main/resources/db/migration`, `infra/docker-compose.yml`, `infra/scripts/deploy-release.sh`, `.github/workflows/ci.yml`, `.github/workflows/cd.yml`, [배포 런북](operations/deployment.md), [알림 정책](notification-delivery-policy.md), [복구 정책](disaster-recovery-policy.md)이다. `ARD.md`에는 현 CD와 맞지 않는 설명이 일부 있으므로 현행 배포 스크립트와 2026-07-20 ADR을 우선 대조한다. `.agents/docs/PRD.md`는 제품 전체 PRD가 아니라 로그 보존 과제 문서다. 이를 전체 기능 우선순위의 근거로 사용하지 않는다.

## 목표·제약·성공 정의

**결정문:** 기존 사용자 흐름을 보존하면서 한 사람이 이해하고 복구할 수 있는 백엔드·운영 구조를 다시 설계한다. 구조 변경은 도메인별로 독립 검증·배포·되돌리기가 가능해야 한다.

| 구분 | 내용 |
| --- | --- |
| 제품 결과 | 검색→강의 상세→구독→좌석 알림, 로그인/탈퇴, 시간표, 리뷰, 피드백/첨부, 관리자 업무의 외부 동작을 명시적 변경 없이는 보존한다. |
| 코드 결과 | 도메인 경계가 실제 데이터·규칙·호출 관계와 맞는지 확인하고, 중복 규칙·불필요한 간접 계층·얽힌 호출 흐름·사용되지 않는 코드 중 증거가 확인된 항목을 우선순위화한다. 이름 변경이나 파일 이동 자체를 개선으로 세지 않는다. |
| 엔지니어링 결과 | 변경 이유·데이터 소유·트랜잭션·실패 처리·복구 순서를 파일과 테스트로 설명할 수 있고, 변경 영향 범위가 예측 가능하다. |
| 운영 결과 | 실제 운영에서 백업/복구, 배포 실패, 알림 지연, 크롤러 실패를 확인하고 대응할 수 있다. |
| 고정 제약 | `apps/web` 파일·UI·호스팅은 수정하지 않는다. 웹 코드는 기존 API 계약과 소비자를 확인할 때 읽기만 한다. 현 웹 클라이언트가 사용하는 API의 경로·메서드·응답·오류·쿠키·권한·페이지네이션을 기본적으로 유지한다. 웹 변경이 필수라면 별도 제품 결정과 배포 순서를 요구한다. |
| 범위 밖 | 화면 개편, 모바일 전용 전환, 새 기능, 프레임워크/언어 교체 자체를 목적으로 한 전면 재작성, 다중 리전·무중단 보장 선언. |

성공은 “코드가 더 예쁘다”가 아니다. **(1)** 현재 핵심 계약의 회귀가 없고, **(2)** 위험한 동작을 재현하는 테스트/운영 실험이 있고, **(3)** 데이터와 파일을 격리 환경에서 실제 복구했으며, **(4)** 배포 실패 후 서비스 복구 절차가 증명되고, **(5)** 각 도메인의 소유권과 의존 규칙을 새 기여자가 따라갈 수 있고, **(6)** 삭제·통합한 코드가 실제 소비자와 런타임 경로를 확인한 뒤 제거되면 성공이다. 설계 단계에서는 코드 구조 감사 결과, 기준선, 선택, 작업 순서, 보류 이유를 기록한다.

---

# 파트 A — 처음부터 설계: 요구, 경계, 선택

## A1. 현 상태와 설계상의 긴장

| 확인된 사실 | 설계상 의미 | 아직 모르는 것 |
| --- | --- | --- |
| Java 21/Spring Boot 3.5.9 단일 프로세스, 기능별 패키지 안의 `presentation/application/domain/infra`, MySQL·JPA·Flyway·Redis를 사용한다 (`apps/server/build.gradle`, `apps/server/src/main/java`). | 분산 서비스보다 패키지 경계·공유 DB·상태 층의 복잡성이 먼저 검토 대상이다. | 변경마다 건드리는 패키지 수, 장애 원인, 실제 팀/운영 비용. |
| 수집은 완전한 응답 검증 후 단일 DB 동기화를 수행하고 좌석 이벤트를 outbox에 적는다 (`CourseCrawlFetcher`, `CourseSynchronizationService`, `SeatNotificationOutboxService`). | 수집·좌석·이력·outbox 원자성은 핵심 보호 계약이다. | 수집 시간, DB write/binlog, 좌석 감지 지연. |
| 알림은 DB outbox/delivery, worker lease, 재시도·DLQ와 Email/FCM/Web Push/Discord 발송을 사용한다 (`notification` 패키지, [정책](notification-delivery-policy.md)). | 처리 상태의 멱등성과 provider의 실제 중복 방지는 다른 문제다. ISSUE-249의 장시간 fan-out은 열린 위험이다. | 채널별 사용량, 지연 분포, 중복 수신율. |
| Google OAuth, JWT access/refresh, Redis refresh/철회 및 Spring Session이 공존한다 (`SecurityConfig`, `AuthService`, `RedisConfig`). | 인증 상태를 단순화할 여지는 있으나 쿠키·로그아웃·장애 의미를 먼저 고정해야 한다. | 외부 API 소비자, Redis 장애 시 기대 동작, 실제 세션 경로. |
| OCI 단일 호스트 Compose에 앱·MySQL·Redis·관측 스택이 있고 웹은 Vercel이다 (`infra/docker-compose.yml`, `.github/workflows/cd.yml`). | 호스트 손실은 상관 장애다. 백업은 단순 성능 최적화보다 앞선다. | 운영 override, 실제 가동 프로필, 자원 사용량. |
| 앱과 Flyway migrator가 MySQL root를 쓴다. Compose에는 서비스별 CPU/memory/PID 한도가 보이지 않는다 (`infra/docker-compose.yml`). | 권한 분리와 자원 예산은 후보이나, 제한값은 피크 측정 없이 정할 수 없다. | 실제 호스트 설정과 자원 피크. |
| CD는 SHA 이미지→SSH staging→기반 서비스 시작→기존 앱 중단→one-shot Flyway→앱 readiness다 (`infra/scripts/deploy-release.sh`). | migration 이후 앱 이미지 재배포는 DB rollback이 아니다. 롤백 가능 스키마를 별도 설계해야 한다. | 최근 배포 실패·중단 시간·복구 시간. |
| 운영 문서는 OCI 로컬 dump를 설명하며 MacBook 수집 스크립트도 있다. `docs/current-system.md`의 로컬 조사에서는 이 Mac의 자동 실행/사본을 확인하지 못했다. | [복구 정책](disaster-recovery-policy.md)의 RPO 24시간·RTO 30분은 **문서상 목표**이며 현행 달성 실적으로 간주할 수 없다. | OCI/다른 off-host 사본의 최신성, DB+첨부 복구 결과. |

ISSUE-258(백엔드 재구축)과 ISSUE-256(격리 개발 서버)은 열린 과제다. ISSUE-246(탈퇴 DB/Redis 불일치), ISSUE-249(lease 초과 fan-out), ISSUE-212(좌석 고변경 데이터 분리)도 설계 입력으로 삼되, 이슈 문장만으로 장애 발생이나 개선 효과를 확정하지 않는다. 과거 ISSUE-100의 복구 시험, ISSUE-101의 자원 제한, ISSUE-099의 Alertmanager 기록은 현재 Compose와 다르다. [인벤토리의 교차검증](current-system.md#과거-이슈와-현재-구현의-교차검증)을 기준으로 재확인한다.

## A2. 우선 품질 시나리오

수치는 **설계 전 합의할 목표**다. 관측 자료 없이 새 SLA를 사실처럼 선언하지 않는다. 각 시나리오의 측정창·분모·예외를 먼저 고정한다.

| 우선 | 자극 / 환경 | 기대 반응과 측정 | 결정 게이트 |
| --- | --- | --- | --- |
| P0 데이터 복구 | OCI 호스트/볼륨이 소실됨 | 격리 호스트에 MySQL·첨부를 복원해 로그인 가능한 계정, 주요 row 관계, 첨부 bytes를 읽는다. 실제 RPO/RTO를 기록한다. | 사업적으로 허용할 RPO/RTO를 다시 승인하고, 그 목표를 만족하는 off-host 경로를 증명하기 전 파괴적 스키마/스토리지 변경 금지. |
| P0 계약 일관성 | 인증/탈퇴에서 MySQL 또는 Redis 실패, OAuth callback 변경 | 계정·토큰·쿠키·HTTP 결과가 정의된 상태로 수렴한다. 실패 주입 테스트와 브라우저 호환 smoke를 통과한다. | 현재 웹의 로그인·refresh·logout·탈퇴 계약을 고정한 뒤 인증 구조 선택. |
| P0 좌석/알림 정확성 | 수집 중단·중복 실행, provider timeout, worker lease 만료 | 강의/이력/outbox의 원자성, 동일 변화 이벤트의 중복 제한, at-least-once 재시도·DLQ, 채널별 중복 가능성을 명시하고 재현한다. | 대표 피크 입력에서 감지→첫 시도 지연과 중복/유실을 계측한 뒤 worker 구조 선택. |
| P1 배포/복구성 | migration 실패 또는 새 이미지 readiness 실패 | 운영자가 원인과 DB 버전을 확인하고, 호환 가능한 앱 버전 또는 forward fix로 복구한다. 실제 중단 시간 기록. | 비호환 migration은 두 릴리스 이상으로 분리하고 복구 리허설 통과. |
| P1 처리 용량 | 수강신청 시기 검색·크롤링·발송이 겹침 | 검색 지연, 수집 실행시간, outbox backlog, CPU/RSS/디스크가 정한 예산 안에 머문다. | 대표 부하·데이터 규모에서 측정해 현재 단일 호스트 유지/분리 판단. |

**측정 계획:** 동일한 대표 기간(평시/수강신청 피크)을 정해 검색 P50/P95·오류율, 수집 총시간/실패율/변화 row 수, 이벤트→첫 발송 시도 P50/P95/P99, delivery 재시도·DLQ·중복 신고, MySQL slow query·row write·binlog, Redis 메모리/재시작, host CPU/RSS/OOM/디스크, 배포 중단/복구 시간을 모은다. 사용자 ID·토큰·수신자·첨부 내용은 계측에 남기지 않는다. 피크 데이터가 없으면 합성 fixture로 기능 위험을 먼저 검증하고 용량 결론은 보류한다.

## A3. 코드 구조를 판단하는 원칙

“도메인을 제대로 나눴는가”는 패키지 이름이나 계층 수만으로 판정하지 않는다. 각 대표 기능의 HTTP/스케줄 진입점부터 규칙, DB·Redis·파일/provider side effect, 실패 처리, 테스트까지 추적하고 다음 증거를 맞춰 본다.

| 점검 관점 | 확인 방법 | 개선 후보로 볼 조건 |
| --- | --- | --- |
| 도메인 소유권 | 기능별 업무 규칙·테이블 read/write·migration·이벤트 생산/소비 목록을 대조한다. | 같은 규칙을 여러 도메인이 수정하거나, 다른 도메인의 entity/repository를 통해 소유 데이터를 직접 변경한다. 읽기 전용 교차 조회는 별도 표시하고 곧바로 위반으로 보지 않는다. |
| 의존 방향 | Java import와 실제 호출을 따라 presentation→application→domain 및 도메인 간 참조를 확인한다. | 순환 참조, 구현 패키지 직접 참조, `common`에 업무 규칙이 역류하거나 특정 기능 변경이 무관한 도메인까지 반복해서 번진다. |
| 흐름 복잡도 | 검색→구독→좌석 감지→outbox→provider 발송, 로그인→refresh/탈퇴, 피드백→첨부 등 대표 흐름에서 호출 수, 상태 변경 위치, 예외 변환, transaction 경계를 기록한다. | 동작·정책을 감추는 wrapper/dispatcher/중복 변환이 있고 이를 제거해도 소유권·실패 처리·테스트 경계가 더 명확해진다. 호출 수가 많다는 이유만으로 제거하지 않는다. |
| 중복 규칙 | 검증·권한·상태 전이·재시도·페이지 규칙이 구현과 테스트에 어디서 반복되는지 비교한다. | 같은 규칙의 수정이 여러 구현에 따로 필요해 불일치 가능성이 확인된다. 값이 비슷할 뿐 의미가 다른 규칙은 중복으로 합치지 않는다. |
| 미사용 코드 | 전체 참조 검색, Spring/Jackson reflection·annotation·component scan, URL/스케줄/설정 등록, test·migration·운영 script, 웹·외부 소비자를 확인한다. | 저장소 내부 정적 참조가 없고 동적 등록이나 외부 계약도 없다는 근거가 함께 있다. 근거가 부족하면 `미확인 후보`로 남긴다. |
| 테스트 구조 | 테스트가 도메인 규칙, 외부 경계, API/DB 계약 중 무엇을 보호하는지 실제 구현과 대조한다. | 의미 없는 mock/중복 assertion이 핵심 동작을 보호하지 않거나, 고위험 분산 side effect에 실패 주입 경로가 없다. 테스트 수 자체는 품질 점수로 쓰지 않는다. |

감사 결과는 **관찰된 사실 / 위험 해석 / 권장 조치**를 분리한다. 각 항목에 파일·symbol·호출 경로, 변경 시 어려움 또는 장애 시 위험, 확신도(높음·중간·낮음)와 근거, 가장 작은 다음 단계를 적는다. 긴 파일·복잡도 수치·AI 작성 이력은 조사 신호일 수 있지만 단독 finding 근거가 아니다. “정리하면 좋아 보임”만 있는 항목은 고치지 않는다.

## A4. 범위·소유권을 새로 정의하는 원칙

1. **한 데이터의 정본은 하나:** `course_key` 같은 논리 참조도 소유자와 삭제/고아 정책을 명시한다. `users`와 관련 PII, `courses`/좌석, 구독, 발송 기록, 첨부 메타데이터의 쓰기 책임을 나눈다.
2. **트랜잭션은 한 DB 안에서 끝낸다:** MySQL commit과 Redis/provider/파일 호출을 한 원자 연산으로 가정하지 않는다. 커밋 이후 작업은 재시도·보상·수렴 정책을 갖는다.
3. **도메인 밖 내부 구현 직접 접근을 줄인다:** 다른 기능의 JPA entity/repository를 바로 변경하는 곳을 찾고, 필요한 명령·조회 계약으로 축소한다. 모든 클래스에 포트를 만드는 식의 형식적 추상화는 하지 않는다.
4. **경계는 배포 단위와 다르다:** 코드 모듈을 나눠도 단일 앱·DB로 운영할 수 있다. 분산 호출/별도 DB는 독립 확장 또는 장애 격리의 실측 이점이 있을 때만 검토한다.
5. **프론트엔드는 계약 소비자:** 내부 구조가 바뀌어도 `/api` 경로와 의미를 유지한다. API breaking change는 별도 결정, 호환 기간, 양쪽 릴리스 순서가 있어야 한다.

## A5. 같은 기준으로 아키텍처 대안 비교

아래는 확정 설계가 아닌 비교 대상이다. 각 후보에 같은 기능 fixture, 장애 주입, CI 시간, 배포/복구 리허설, 변경 시나리오를 적용한다.

| 후보 | 경계/배포/데이터 | 얻는 것 | 비용·위험 | 선택 조건 |
| --- | --- | --- | --- | --- |
| **A. 현 feature 패키지 정리** | 단일 Spring Boot·단일 MySQL. 계층 의존 규칙과 public API만 고정. | 가장 작은 이전·롤백 비용. 현재 transaction 보존. | 컴파일 경계가 약해 다시 교차 참조 가능. | 교차 의존이 적고 아키텍처 테스트로 충분히 막힐 때. |
| **B. 경계가 명시된 모듈러 모놀리스** | 단일 배포·DB, 도메인별 공개 인터페이스/소유 테이블. 필요할 때만 빌드 모듈 경계. | 코드 탐색·테스트·변경 영향 통제, 운영 형태 유지. | 초기 경계 정리와 공용 조회 설계 비용. 모듈을 과도하게 쪼개면 빌드·인지 비용 증가. | 변경이 여러 패키지에 번지고 책임 혼선이 반복될 때. **잠정 기준안.** |
| **C. 선택적 포트/어댑터** | B의 고위험 외부 경계(JUMP, provider, 파일, 시계)에만 포트 추가. | 실패 주입과 교체 실험 쉬움. | 범용 인터페이스 남발 시 타입·매핑 증가. | 현재 테스트에서 외부 의존을 격리하기 어려운 경로에 한정. B와 결합 가능. |
| **D. 서비스 분리/새 런타임** | 독립 배포·프로세스, 경우에 따라 데이터 분리. | 실측된 독립 용량/장애 격리 가능. | 네트워크·관측·인증·데이터 일관성·비용·운영 부담 급증. | 단일 호스트와 모듈 경계로 해결 불가한 용량/격리 요구가 증명될 때만 별도 ADR. |

**잠정 권고:** A를 최소 비교선으로, B를 목표 후보로 삼고 C를 수집/발송/파일에 필요한 만큼만 적용한다. Java/Spring/MySQL/Redis와 단일 OCI를 즉시 교체할 근거는 아직 없다. D나 프레임워크 교체는 “새 출발”이라는 느낌만으로는 채택하지 않는다. 이 권고는 복구 목표, 운영 비용, 도메인 간 변경 측정에 따라 바뀐다. 선택 ADR에는 두 후보의 변경 예시, 테스트 작성량, CI 시간, 런타임 지표, 롤백 난도를 함께 기록한다.

## A6. 백엔드 논리 경계 제안

현재 feature 패키지는 데이터·규칙 소유가 완전히 분리됐다는 뜻이 아니다. 예를 들어 `course ↔ crawling`은 수집 실행 상태를 강의 조회에서 읽고 수집 동기화가 강의 저장소를 직접 쓰는 양방향 참조다. 알림 처리는 사용자·구독 JPA 저장소를 직접 사용하며, `admin_action_logs`는 피드백 패키지 안에 있지만 알림 재처리도 기록한다.

목표 논리 경계, 테이블 소유자, 교차 참조 규칙, 근거와 미확인 사항은 [백엔드 논리 도메인 경계 초안](decisions/2026-09-29-server-domain-boundaries.md)에 기록했다. 현재 제안은 같은 Spring Boot 앱·MySQL 안에서 테이블마다 쓰기 소유자를 하나로 하고, 다른 경계는 식별자와 좁은 application 계약을 통해 협력하는 것이다. Gradle 멀티모듈이나 별도 서비스를 지금 만들자는 지시는 아니다.

특히 수집 동기화에서 강의/좌석 이력과 알림 outbox를 같은 transaction에 커밋하는 기존 의미를 보존해야 한다. 전체 import·read/write 경로를 더 확인한 후 한 경계씩 적용한다. `common`은 실제 공유되는 HTTP·인증·시간 등 기술 요소만 담당하고 도메인 규칙이나 저장소를 소유하지 않는다.

```text
브라우저(Vercel) → 기존 /api 계약 → 단일 서버 앱 → MySQL(정본)
                                            ├→ Redis(세션/토큰 상태)
                                            ├→ JUMP(수집)
                                            ├→ Email/FCM/Web Push/Discord(발송)
                                            └→ 첨부 volume
크롤러: fetch/검증 → DB 동기화·좌석 이력·outbox commit → worker claim → provider → delivery 정산
배포: GHCR SHA → OCI Compose → Flyway one-shot → 앱 readiness; NPM edge와 Vercel은 별도 운영 경계
```

---

# 파트 B — 보호막과 기준선

## B1. 실제 계약을 먼저 잠근다

작업 B1의 산출물은 “현재 기능이 무엇인가”를 테스트 가능한 목록으로 만든 표다. [API 인벤토리](current-system.md#http-api-inventory)에서 모든 controller와 프레임워크 경로를 추출하고 다음을 각 엔드포인트에 기록한다: 소비자(웹/관리자/외부 불명), 인증·권한, 요청/응답 예시(민감정보 제거), 상태코드/오류코드, null/기본값, 정렬·페이지, 쿠키/SameSite/CORS, 파일 헤더, rate/크기 한도, 부수효과, DB transaction. 웹을 읽어 계약을 확인할 수 있으나 `apps/web` 파일은 수정하지 않는다.

고위험 characterization 테스트는 **현재 동작을 정확히 기록**하되 잘못된 동작을 영구 사양으로 만들지 않는다. 버그가 의심되면 현상 테스트와 원하는 계약 테스트를 분리하고 설계 결정을 남긴다.

| 흐름 | 반드시 고정할 예시 |
| --- | --- |
| 인증 | Google callback, 신규/기존 사용자, access 만료→refresh 회전, logout, 권한 없는 관리자 호출, Redis 중단, 탈퇴 직후 token 사용. |
| 강의 | 복합 필터·요일/시간·정렬·페이지, 학기 경계, `course_key` 인코딩, 부분 수집/잘못된 upstream 응답에서 기존 데이터 보존. |
| 알림 | 좌석 열림/닫힘/변경 없음, 동시 수집, transaction rollback, provider timeout/부분 성공, lease 초과, DLQ replay. |
| 사용자 데이터 | 구독·찜 중복/삭제, 시간표 고아 강의, 리뷰 권한, 피드백 multipart 제한·소유자/관리자 다운로드·삭제. |
| 운영 | Flyway 빈 DB/이전 스키마, app readiness, 배포 중단 단계별 복구. |

기존 테스트 기반은 `.github/workflows/ci.yml`의 `./gradlew clean check migrationTest bootJar` 및 서버 테스트군이다. 새 테스트는 누락된 계약과 실패 경계에 집중한다. H2만으로 MySQL collation/locking/Flyway를 대신하지 말고 필요한 경로는 Testcontainers MySQL·Redis를 사용한다. 외부 provider는 계약 스텁/가짜 서버로 실패를 재현하고 실제 발송은 격리 환경의 제한된 smoke로 확인한다.

## B2. 비용·위험 지도를 만든다

도메인마다 `변경 빈도 × 결함 영향 × 이해 비용 × 제거/롤백 비용`을 **정성 등급**으로 기록하고 근거 PR/이슈·최근 수정 파일 수·장애 기록을 붙인다. 활동량이 없으면 “미측정”으로 둔다. 예비 우선순위는 백업/복구, 탈퇴 정합성, 알림 lease, 배포 migration, 수집 원자성, 인증 복잡성, 파일 복구, 검색/DB 성능이다. 코드 구조 감사는 설계 결정 전에 수행하고, 확인된 코드 부채는 해당 도메인 이전과 함께 처리한다. 운영 데이터와 감사 결과에 따라 순서를 재정렬한다.

기준선 수집 산출물: (1) 핵심 API 계약표, (2) 도메인별 dependency/import 및 테이블 read/write 지도, (3) 주요 흐름의 진입점→규칙→side effect→실패 처리→테스트 추적도, (4) 중복·미사용·간접 계층 후보와 소비자 검증 기록, (5) migration·볼륨·secret 목록, (6) 2개 대표 기간의 계측 보고서, (7) 복구/배포 실험 기록, (8) 확인/기각/미확인 부채 목록. `NotificationService.handleSeatOpenedEvent`, Batch 메타데이터, 중복 테스트 알림 API 등은 [인벤토리](current-system.md#리팩토링-후보와-위험)의 **제거 후보**일 뿐 사용 증거와 호환성 확인 전 삭제하지 않는다.

## B3. 증거로 후보를 줄이고, 결정적인 가정만 시험한다

모든 후보를 실제로 구현하는 비교 실험은 하지 않는다. 각 결정을 바꿀 수 있는 주장만 먼저 적고, 이미 있는 강한 증거로 탈락시킬 수 있는 대안은 구현하지 않는다. 특히 새 기술의 단순 benchmark는 운영 복잡도·복구성·호환성을 대신 증명하지 못한다.

### 증거 우선순위

| 증거 | 무엇을 증명하는가 | 한계/사용법 |
| --- | --- | --- |
| 현재 코드·설정·import·호출 경로 | 현재 프로세스/DB/API의 구조, 실행 책임, 구성된 제한의 존재 | 실제 운영 반영·호스트 자원·요청 빈도는 증명하지 않는다. 현재 소스와 운영 manifest를 같이 확인한다. |
| 재현 가능한 테스트·migration·운영 이력 | 해당 입력/실패 조건에서 동작하거나 실패했다는 사실 | 과거 통과나 이슈 종료가 현 HEAD·현재 운영까지 증명하지 않는다. 최신 SHA와 현재 schema로 재현한다. |
| 운영 지표·로그·장애 기록·청구 내역 | 실제 부하·실패 빈도·비용의 관측값 | 기간·표본·수집 누락을 함께 적는다. 데이터가 없으면 “없음”이 아니라 “미측정”이다. |
| 격리된 최소 실험 | 아직 불확실한 특정 인과/위험을 가리는 결과 | 한 가설만 시험한다. 운영 데이터·실제 provider 발송은 기본 금지하고, fixture/fake를 쓴다. |
| 가정/전망 | 조사 전 임시 방향 | 출처·확신도·결정을 바꿀 증거를 함께 적고, 확인 전에는 비가역 선택의 근거로 삼지 않는다. |

각 항목을 **결정적 사실 / 강한 관측 / 제한된 관측 / 가정 / 미확인**으로 표기한다. 수치 목표는 현재 지표와 사용자가 허용하는 결과를 보고 정하고, 목표가 없으면 “기준선 회귀 없음”으로 임시 판정한다. 옵션마다 동일한 실험을 억지로 만들지 않는다. 낮은 위험·되돌리기 쉬운 변경은 기존 테스트와 코드 근거로 결정할 수 있다.

### 분리 아키텍처에는 한 개의 최소 반증 실험부터

현재 `apps/server/src/main/java/bhoon/sugang_helper/SugangHelperApplication.java:12-16`이 스케줄링을 활성화하고, `academic/crawling/application/CourseScheduler.java:41-46`과 `seatalert/notification/application/SeatNotificationOutboxWorker.java:59-69`가 같은 Spring 애플리케이션 안에서 `@Scheduled`로 실행된다. Compose에는 서버 `app` 서비스가 하나뿐이다 (`infra/docker-compose.yml:65-123`). 따라서 **JVM/container가 종료되는 실패는 현재 API도 함께 중단시킨다**는 점은 코드/배포 구조로 확인할 수 있다. 반면 일반 수집 예외가 실제로 API 중단을 일으켰는지는 확인되지 않았다. `academic/crawling/application/CourseCrawlerService.java:152-163`은 실패를 기록하고 runtime exception을 다시 던지지만, 스케줄러 수준에서 그 예외가 어떻게 처리되는지는 현재 테스트/설정에서 입증하지 못했다. 일반 job 실패와 JVM 종료를 같은 위험으로 취급하지 않는다.

자원 제한 근거에도 저장소 내부 충돌이 있다. 현 `infra/docker-compose.yml`의 `app`에는 CPU/memory/PID 제한이 없지만, `docs/troubleshooting.md:147-151`와 ISSUE-101 로그는 과거 제한 적용을 설명한다. 현재 저장소 상태는 Compose 파일, 운영 런타임 상태는 실제 Compose override/호스트 설정으로 각각 확인해야 한다. `CourseCrawlerService.java:32,65-82`의 중복 방지는 인스턴스 내부 `AtomicBoolean`이다. 크롤러를 여러 프로세스로 띄우면 이 플래그를 공유하지 않으므로, 초기에 crawler role은 단일 인스턴스로 유지하고 다중 실행이 필요해질 때 DB 기반 lease/분산 lock을 별도 설계한다.

| 주장 | 기존 자료로 판별 | 남은 불확실성에만 할 실험 | 판정 |
| --- | --- | --- | --- |
| 일반 수집 실패가 API를 중단시킨다 | 수집은 실패 기록 후 runtime exception을 재던진다. scheduler exception 처리/다음 주기 지속은 설정·테스트에서 아직 입증되지 않았다. | API context가 살아 있는 상태에서 실패를 주입하는 작은 integration test 한 번. | job 실패 처리와 JVM 격리가 필요한지 구분. |
| 크롤러 JVM/container 종료가 API를 중단시킨다 | 현재 scheduler와 API가 같은 Spring app/container다. | 별도 crawler role에서 worker process 종료 후 API health/read 경로 확인. 대안 전체 구현은 불필요. | process 단위 분리를 정당화하는 구조 근거. |
| 크롤러의 CPU/RSS가 API 품질을 떨어뜨린다 | 피크 수집 때 host/app CPU·RSS, API 지연/오류, DB 연결·쿼리 지표를 같은 시간축으로 확인. | 관측이 없으면 격리 Compose에서 API+분리 worker를 띄우고 fixture 수집 부하/worker kill을 한 번 재현. API synthetic probe와 host 여유·DB pool을 관측. | 같은 OCI에서 역할을 분리하는 것으로 충분한지 판단. Compose/호스트가 실제 자원 한도를 적용하는지 따로 검증. |
| worker를 다른 호스트에 둬야 한다 | 같은 호스트의 OOM/CPU 압박·디스크 또는 호스트 장애 기록, 합의된 복구/가용성 목표와 비교. | 같은 호스트의 process/resource 격리로 요구를 만족하지 못할 때에만 비용/복구를 비교하는 host 분리 실험/계산. | host 장애 영역 분리는 측정된 필요 또는 명시된 복구 목표가 있을 때 검토. |
| 알림 프로세스 분리가 필요하다 | 현재 outbox backlog/처리 지연/provider 실패, API와 worker 자원 경쟁, lease 초과 이슈·테스트를 확인. | backlog 또는 lease 위험이 남으면 단일 worker 분리와 장시간 fan-out/실패를 재현. 모든 provider를 각각 end-to-end 재검증하지 않고 공통 delivery contract + provider별 adapter contract를 확인. | DB outbox를 쓰는 이유와 별도 메시지 브로커 필요성은 별도 판단. |

이 최소 실패 실험은 **전체 혼합 부하 시험을 대체하지 않는다**. 먼저 한 빌드 산출물을 `api`, `crawler`, `notification-worker` 역할로 실행해 crawler 종료/제한된 자원 압박 중 API가 살아 있는지 확인하고, 별도로 아래의 API+크롤링+알림 동시 시나리오를 현재 구조와 후보 구조에서 비교한다. 기존 API·알림·수집 계약은 각각 기존 테스트로 보호한다.

이 시험이 통과해도 **공유 호스트·MySQL의 장애 격리는 증명되지 않는다**. host 분리는 같은 호스트 자원 격리로 요구를 못 맞추거나 실제 호스트 장애가 목표를 위반할 때만 후보로 올린다. 분리 실험이 API 안정성을 개선하지 못하거나 DB 병목만 드러내면 추가 컨테이너가 답이 아니므로 DB 동시성·쿼리·작업량을 먼저 제한한다.

### 로컬 k6 기준선·혼합 부하·크롤러 격리 시험

**질문:** API 이용, 5분 크롤링, 알림 fan-out이 같은 CPU와 DB를 함께 쓸 때도 API가 버티는가? 이 세 작업이 겹친 정상·높은 부하에서 worker를 분리하면 사용자 요청과 backlog 회복이 나아지는가? “최악”은 무제한 입력이 아니라 **운영 기록으로 뒷받침되는 가장 높은 이용량과 알림 fan-out이 크롤러 실행과 겹치는 조건**으로 정의한다. 잃어버린 7~8월 피크는 복원하지 못하며, 9월 변경 기간에 관측한 가장 바쁜 한 시간의 평균 약 0.41 API 요청/초만 낮은 총량 기준으로 쓴다. 이 수치를 순간 최대치, 동시 사용자 수, API별 분포로 해석하지 않는다.

**로컬 사전 점검 (2026-09-28):** Docker Compose 설정 검증이 통과했고 k6 CLI가 설치되어 있다. Docker Desktop은 ARM64, 8 CPU, 약 7.65 GiB 메모리다. `ssh zupzup` 읽기 전용 확인에서 운영 Linux는 ARM64, 1개 CPU, Docker 메모리 약 5.77 GiB(호스트 `MemAvailable` 약 3.44 GiB)로 관측됐다. CPU 명령어 집합은 같지만 코어 수·CPU 성능·디스크·네트워크가 달라 로컬 수치를 운영 용량으로 직접 환산할 수 없다. 처음 확인할 때는 서비스 컨테이너와 k6 스크립트가 없었다. 격리용 Compose overlay, k6 스크립트, 전용 네트워크·볼륨은 현재 준비되어 있다.

**운영 자원 스냅샷 (2026-09-28):** OCI에서 앱·MySQL·Redis·NPM·Prometheus·Loki·Alloy·Grafana 컨테이너가 실행 중이었다. `docker inspect`상 모두 Docker CPU·메모리·PID 제한이 미설정(`0`/unset)이다. 같은 순간의 `docker stats` 메모리 합은 약 1.78 GiB(app 599 MiB, MySQL 568 MiB, 관측 컨테이너 약 538 MiB, NPM 115 MiB, Redis 4 MiB)였고 각 컨테이너 CPU는 0.5% 미만이었다. 이는 낮은 부하 시점의 단일 스냅샷이며 피크·OOM 이력이나 7~8월 사용량을 설명하지 않는다. 별도 컨테이너는 프로세스 실패 경계를 제공하지만, 현 운영 설정에서는 CPU·메모리 고갈을 컨테이너별로 막지 않는다. 운영 호스트에 k6를 실행하지 않는다.

1. **환경·트래픽 근거:** 전용 Compose 네트워크·MySQL·Redis·업로드 볼륨만 쓴다. 현재 가진 0.41 req/s는 전체 요청의 한 시간 평균이므로 최종 혼합 부하의 API별 비율로 사용하지 않는다. 2026-09-28 확인값은 회원 673명, 바쁜 날 DAU 약 278명, 활성 구독 1,625건이다. 목표 1,000명·400 DAU의 요청률은 사용자당 요청률이 유지된다는 가정으로 약 0.59 req/s로 환산한다. 경로별 요청 비율과 강의별 구독 분포는 여전히 미확인이라 혼합 비율·강의별 fan-out 합성 가정을 명시한다.
2. **데이터·외부 부수효과:** 운영 dump, 실제 계정·토큰·첨부를 복제하지 않는다. 로컬 DB에 합성 사용자·구독·기기를 만든다. 실제 JUMP는 사용자가 허용한 정상 5분 주기로만 호출하고 과거 3년 startup crawl은 끈다. 실사용과 같은 데이터 수집 경로는 이 시험에 포함할 수 있지만, 실제 좌석 변동이 항상 발생한다고 가정하지 않는다. notification 채널은 실제 provider에 연결하지 않는다. 테스트 프로필에서 provider sender를 가짜 sink로 강제하고, 전송 요청 수·지연·실패를 기록한다. 실제 provider bean이 선택되지 않았음을 먼저 검증한다.
3. **단계 A—단독 진단:** 현재 1 VU·1분 search smoke는 완료했다. 검색 단독 0.41 req/s·15분과 0.8/1.6/3.2 req/s·각 5분은 검색/DB 쿼리 병목을 찾는 진단용이다. 이 결과만으로 아키텍처를 결정하지 않는다. 더 의미 있는 총량과 API별 분포가 관측되면 진단 부하는 보정한다.
4. **단계 B—동시 정상 부하:** k6에는 관측된 경로 비율대로 과목 검색·상세·이력 등 핵심 API 흐름을 섞는다. 동시에 실제 JUMP 5분 크롤링과 합성 notification outbox batch/fan-out을 실행한다. 적어도 3회 크롤링 주기 동안 유지해 같은 CPU·DB에서 겹치는 구간을 관측한다. 현재 1,625건 fan-out 시험에서 worker queue 거절이 확인되어 다음 부하 단계는 원인 확인 뒤 재개한다. 상세 결과는 `docs/performance/2026-09-28-mixed-load-1000-400.md`에 기록했다.
5. **단계 C—재현 가능한 전체 경로:** 로컬 JUMP stub이 1차 응답에서 좌석 0, 다음 응답에서 좌석 1을 반환하게 한다. 합성 구독자가 있는 상태에서 `크롤링→좌석 이력→outbox→delivery→가짜 sink 기록`의 건수를 대조하며 API 혼합 부하를 겹친다. 이를 통해 실제 upstream 상태와 무관하게 좌석 변화 알림 경로가 끝까지 처리되는지 검증한다.
6. **단계 D—동시 실패 조건:** 단계 B가 안정적이면 API 부하 중 가짜 provider 지연/timeout으로 outbox backlog를 만들고 회복·재시도·DLQ·중복 발송 의미를 확인한다. crawler 지연/예외와 worker 강제 종료도 각각 주입해 readiness, API 오류·지연, 복구 시간, backlog 수렴을 본다. 먼저 실패를 하나씩 주입해 원인을 구분하고, 마지막에 필요한 조합만 함께 시험한다.
7. **구조 비교와 기록:** 현재 구조와 역할 분리 후보에 같은 API 비율·크롤러 입력·notification 양·총 자원 예산을 적용한다. k6 요청률·오류·P50/P95/P99·dropped iteration, 크롤러 실행시간·변화/쓰기 수, outbox 적체량·가장 오래된 항목·처리량·retry/DLQ, Hikari active/pending, DB lock/CPU, 컨테이너 CPU/RSS/restart/OOM/readiness를 함께 기록한다. 안정 구간은 각 구조에서 3회 반복한다. readiness 실패, 오류 증가, 재시작/OOM, k6 dropped iteration이면 다음 부하 단계로 진행하지 않는다. 기준 latency·복구 시간 목표가 정해지기 전에는 합격 숫자를 임의로 만들지 않는다.

**현재 셋업/측정 상태 (2026-09-28):** 격리 Compose, mixed k6 흐름, 1,000개 합성 계정, 내부 Mailpit sink는 구현하고 실행했다. 낮은 비교 실행은 100명씩 세 번의 fan-out을 완료했다. 목표 실행은 0.59 req/s와 현재 활성 구독 1,625건을 한 번 겹쳤다. 165개 API 요청은 모두 성공했고 1,625개 delivery도 결국 모두 SENT 됐으나, worker executor queue가 가득 찬 상태에서 `RejectedExecutionException`이 41회 발생했고 처리에 322초 걸렸다. 오류 확인 후 안전 기준에 따라 실행을 4분 38초에 중단했다. 목표 2,415건과 높은 API 단계는 아직 실행하지 않았으며, 이번 결과로 서비스 용량이나 worker 분리 효과를 판정하지 않는다. 다음에는 queue 거절을 재현·해결하고 현재 fan-out을 재검증한 뒤 3회 주기 시험을 진행한다. 결정적 JUMP stub 시험도 아직 별도로 실행해야 한다. 혼합 부하 결과는 `docs/performance/2026-09-28-mixed-load-1000-400.md`, search smoke는 `docs/performance/2026-09-28-local-api-crawler.md`에 있다.

**판정 경계:** 컨테이너 역할 분리로 API와 크롤러 프로세스 종료, 제한된 CPU/메모리 경쟁이 완화되는지 확인할 수 있다. 공용 OCI 호스트, MySQL, 디스크, 네트워크의 장애 격리는 이 시험으로 증명되지 않는다. 로컬 수치는 같은 Mac/Docker Desktop 조건에서 구조 간 비교에 사용하고, OCI 절대 용량 판단은 운영 OCPU·메모리 한도와 비교 가능한 환경에서 다시 확인한다. 각 실제 측정은 조건·표본·해석 한계를 `docs/performance/YYYY-MM-DD-local-api-crawler.md` 형식으로 남기고, 원시 summary도 보존한다.

### 실험 전·후 결정 기록

최소 실험은 다음만 미리 고정한다: **가설 하나**, 분리 대상/공유 자원, 입력 fixture와 부하, 정상·실패 자극, 측정 항목, 통과/실패 판정, cleanup·되돌림. 같은 조건의 현재 baseline과 한 번 비교한다. 결과가 애매하면 가능한 한 한 변수만 바꿔 재실험하고, 그래도 애매하면 확장 배포를 보류한다. 여러 구조를 동시에 구현하거나 실험 환경의 성공만으로 운영 안전을 선언하지 않는다.

결정표에는 대안마다 근거 링크/날짜, 확신도, 사용자 영향, 월 반복 비용, 추가 운영 작업, 되돌림 비용, 미확인 가정을 남긴다. 강한 사실로 후보를 걸러낸 뒤 위험도가 높은 가정에만 최소 실험을 한다. 최종 선택은 “모든 대안 중 이론상 최고”가 아니라 **요구를 만족하면서 가장 단순하고 되돌리기 쉬운 후보**로 한다.

---

# 파트 C — 코드 구조·도메인 경계·백엔드 로직 재구성

## C1. 공통 구현 규칙

- HTTP controller는 입력 검증·인증 주체·응답 변환을 담당한다. 도메인 규칙과 provider 호출은 application 명령에 둔다. 예외→HTTP 매핑은 한 곳에서 유지하되 도메인별 오류 의미를 잃지 않는다.
- 쓰기 use case는 명시적 transaction 경계와 멱등성 키를 가진다. 읽기 모델은 성능상 필요한 직접 SQL/QueryDSL을 허용하되 누가 소유하고 캐시 무효화가 필요한지 기록한다.
- repository 인터페이스는 테스트/도메인 경계를 실제로 보호하는 곳에만 둔다. 현 application 계층의 domain repository/infra JPA 직접 주입 혼재를 조사한 뒤 **한 규칙**을 선택한다.
- 외부 I/O(JUMP, provider, 파일, Redis)는 timeout·retry budget·실패 분류·로그/metric을 정의한다. 무한 재시도나 DB transaction 안의 느린 네트워크 호출은 허용하지 않는다.
- 순수 도메인 판단(좌석 변화, 알림 대상, 탈퇴 상태, 파일 보존)은 시계와 외부 I/O로부터 분리해 단위 테스트한다. 단순 CRUD를 위해 불필요한 계층·DTO 매핑을 늘리지 않는다.

### 코드 부채 후보의 처리 기준

| 후보 | 확인할 근거 | 허용되는 다음 조치 |
| --- | --- | --- |
| 경계 누수 | 도메인별 규칙·테이블 read/write·import·호출·이벤트 흐름. 교차 읽기와 교차 쓰기를 구분한다. | 소유자를 정하고 write 경로를 하나로 줄인다. 교차 read는 계약·성능·필요성이 확인된 경우 유지할 수 있다. |
| 얽힌 흐름 | entry point에서 상태 변경, 트랜잭션, 네트워크/파일 I/O, 예외 변환, 테스트까지 연결한다. | 책임·실패 경계를 보존하면서 한 조각씩 분리하거나, 의미를 감추는 중계 계층을 제거한다. 클래스 분리 자체는 목표가 아니다. |
| 중복 규칙 | 권한·검증·상태 전이·재시도 등의 구현과 테스트를 비교한다. | 같은 제품 규칙이라는 근거가 있을 때 단일 진실 공급원으로 통합한다. 비슷해 보이는 규칙을 성급히 공통화하지 않는다. |
| 미사용 후보 | 참조 검색에 더해 Spring/Jackson reflection·annotation·component scan, URL·스케줄·설정 등록, 테스트·migration·운영 script, 웹/외부 소비자를 확인한다. | 동적·외부 소비자가 없다는 근거를 남긴 뒤 제거한다. 미확인 항목은 삭제하지 않고 후보로 기록한다. |
| 추상화·테스트 과잉 | 인터페이스/래퍼가 실제 격리·교체·소유권을 제공하는지, 테스트가 어떤 계약을 보호하는지 확인한다. | 경계 보호가 없는 중계 계층이나 계약을 보호하지 않는 중복 검사를 줄인다. 테스트 수나 파일 길이로 품질을 판정하지 않는다. |

각 finding은 파일·symbol·호출 경로, 관찰 사실, 이해/변경 위험, 확신도와 이유, 최소 수정 단위, 검증 방법을 기록한다. 좁고 동작 보존 가능한 항목은 관련 baseline test를 전후 실행한다. API·스키마·권한·배포 동작 변경이 필요한 finding은 정리 PR에 섞지 않고 별도 설계 결정과 호환 계획을 거친다.

### 바텀업 적용 순서와 단위

조사는 대표 요청/스케줄 흐름을 **진입점부터 아래로** 따라가 전체 계약과 의존 관계를 먼저 파악한다. 실제 변경은 한 번에 넓은 계층을 갈아끼우지 않고, 소유권이 확인된 작은 규칙·모델부터 호출자 방향으로 올려 붙인다.

1. 한 도메인 안에서 작은 동작 하나를 고르고 기존 입력·출력·불변조건을 테스트로 고정한다.
2. 순수 규칙이나 값 표현을 정리하고 단위 테스트를 통과시킨다.
3. 저장 모델을 건드릴 필요가 있으면 그 테이블의 소유자, 관계, null/delete 의미, 현재 migration과 transaction을 먼저 확인한다. JPA `@Entity`는 DB 매핑이므로 그 자체를 도메인 경계로 간주하지 않는다.
4. repository와 application 흐름을 연결하고, DB·Redis·파일·외부 provider의 side effect 및 실패 의미를 검증한다.
5. 마지막으로 controller·scheduler·다른 도메인 호출자에 통합하고 기존 API/운영 계약과 end-to-end 동작을 확인한다.
6. 한 조각이 안정되면 이전 경로를 제거하고 다음 조각으로 간다. 공통 추상화·다중 도메인 이동·대규모 schema 변경은 별도 근거와 전환 계획 없이는 끼워 넣지 않는다.

따라서 바텀업은 **모든 entity를 먼저 일괄 재작성한다**는 뜻이 아니다. 공유 참조나 소유권 혼선이 있는 entity는 먼저 read/write 지도를 만들고, 해당 소유 도메인과 소비자를 함께 다룰 수 있을 때만 변경한다. 초기 리팩토링 조각은 감사 결과에서 작고 되돌리기 쉬우며 대표 기능을 검증할 수 있는 것으로 고른다. 인증·탈퇴, crawler/outbox, 알림 lease, 스키마 소유권처럼 실패 비용이 큰 흐름을 단지 코드상 아래 계층이라는 이유로 첫 대상으로 삼지 않는다.

## C2. 도메인별 설계 항목 (실행 순서는 감사 후 선택)

| 분야 | 설계 질문과 변경 단위 | 검증·전환 조건 |
| --- | --- | --- |
| 인증/Identity | 실제 로그인 상태 정본(JWT/refresh registry/Spring Session)을 요청별로 추적한다. 세션 단일화, 현 혼합 유지, JWT 중심 중 하나를 **외부 클라이언트·로그아웃 즉시성·Redis 장애** 기준으로 비교한다. 탈퇴의 DB commit/Redis 철회를 명시적 상태 전이 또는 durable 재시도로 설계한다. | OAuth/refresh/cookie/CSRF/CORS/권한 계약 테스트, Redis/DB 실패 주입, 탈퇴 후 철회 수렴. ISSUE-246의 차단 조건 해소 전 삭제/철회 순서 변경 금지. |
| 수집·좌석 | JUMP fetch, parser, 전체 결과 검증, 수집 실행 제어, DB sync를 구분한다. snapshot version/run ID, 중복 실행·부분 실패·학기 전환 의미를 정의한다. course/availability 분리는 ISSUE-212의 DML·binlog 비교가 유의미할 때만 한다. | 동일 fixture의 course/schedule/history/outbox 결과 동일성, 0-change write 0 여부, 피크 수집 시간/DB write 비교, migration 복구. |
| 구독·알림 | 이벤트 식별/수신자 선정/채널 발송/정산을 나누고 lease 갱신 또는 bounded fan-out을 비교한다. provider가 성공했으나 ack를 잃은 경우 중복 가능성을 API/운영 계약에 명시한다. target별 상태가 필요한지 사용량·중복 영향으로 판단한다. | 다중 worker의 lease 초과·부분 성공·재시도·DLQ replay 테스트, outbox backlog/첫 시도 지연/발송 중복 관측. ISSUE-249가 핵심 gate. |
| 파일·피드백 | DB metadata와 `app_uploads` bytes의 생성/삭제 실패 보상을 정리한다. 사용자/관리자 다운로드의 권한·감사 경계를 유지하고, 파일 저장소 후보(현재 volume+off-host, object storage 등)를 복구/비용으로 비교한다. | JPEG/PNG 및 크기/픽셀 제한, 경로 순회/권한, DB 실패 시 orphan, DB row+파일 동시 복원, 삭제 후 접근 불가 테스트. |
| 검색·조회 | QueryDSL 유지 vs 단순 SQL/JPA를 실제 필터 조합과 `EXPLAIN`, P95로 비교한다. N+1·페이지 안정성·인덱스를 확인한다. | API 응답/정렬 동일, 대표 규모에서 latency와 DB load가 회귀하지 않음. 측정 전 의존성 교체 금지. |
| 시간표·리뷰·콘텐츠·관리자 | 데이터 소유와 `course_key` 참조 의미를 고정하고 CRUD/권한/감사 일관성을 정리한다. 중복 endpoint 또는 미사용 코드의 소비자를 확인한다. | 기존 응답/권한·리뷰 scope·관리자 감사·시간표 삭제 시나리오 통과. 기능별 작은 PR로 이전. |

한 변경 단위는 **한 소유권 경계 또는 한 실패 경계**다. 패키지 이동, 스키마 변경, API 의미 변경을 한 PR에 섞지 않는다. 구현 전후 같은 contract suite를 실행하고, 한 단계가 안정화되기 전 다음 단계에서 기존 코드를 삭제하지 않는다.

### 주요 실행 경계의 실패 계약

| 경계 | 정본/원자 단위 | 실패 감지 | 재시도·복구와 금지 사항 |
| --- | --- | --- | --- |
| JUMP→수집 | upstream 전체 snapshot을 검증한 뒤 한 번의 DB 동기화 | 응답 형식·개수·중복 키·완료 여부, 마지막 성공 시각 | 부분 응답으로 기존 강의를 대량 삭제하지 않는다. retry는 동일 run의 중복 반영을 막고, 실패 시 이전 snapshot을 유지한다. |
| 좌석→outbox | 강의 좌석·이력·outbox의 DB commit | 변화 건수와 outbox 생성 건수 대조, rollback 주입 | DB commit 전 provider 호출 금지. 재수집 시 동일 변화의 이벤트 식별/중복 정책을 고정한다. |
| outbox→delivery→provider | DB claim/상태 갱신과 별도의 외부 I/O | lease 만료, timeout, 4xx/5xx, partial fan-out, backlog age | bounded retry·DLQ·선택 replay. lease fencing은 상태 덮어쓰기만 막으므로 외부 중복 가능성을 숨기지 않는다. |
| 계정 탈퇴→Redis 철회 | MySQL 사용자 상태와 Redis token 상태는 분산 경계 | DB/Redis 실패 주입, 탈퇴 후 token 재사용 검사 | 실패 시 계정·인증 상태가 일관되게 보존되거나 durable 수렴해야 한다. 커밋 전 철회를 성공 처리하지 않는다. |
| 피드백 row→파일 bytes | DB metadata와 별도 volume | orphan 파일/누락 파일 검사, 첨부 read 실패 | 쓰기 실패 시 파일 보상, 삭제 실패 시 재시도·기록. DB만 복원해도 첨부가 복구됐다고 선언하지 않는다. |

## C3. 데이터·캐시·마이그레이션 규칙

MySQL이 업무 데이터의 정본이다. Redis는 세션·토큰 철회/refresh 상태를 소유하지만 DB와의 합의가 필요한 상태는 단순 cache로 간주하지 않는다. 첨부 bytes는 DB와 별도 일관성 경계를 가진다. 테이블 소유자와 읽기·쓰기 권한 목록을 작성한 뒤 cross-domain 쓰기를 없애는 순서를 정한다. `flyway_schema_history`와 V1~V28 migration은 변경하지 않는다.

1. **Expand:** 새 nullable 컬럼/테이블/인덱스 추가. 현 앱과 새 앱 모두 읽을 수 있어야 한다. 기존 컬럼 삭제나 의미 변경 금지.
2. **Migrate:** bounded 배치로 backfill; row count·checksum 또는 대표 키 비교, NULL/고아·중복 검사. 부하와 중단 가능 시점을 기록. 장시간 DDL lock과 binlog 증가를 사전 검증한다.
3. **Switch:** 새 코드가 양쪽 스키마와 호환되는 기간을 두고 읽기/쓰기 전환. dual write가 필요하면 실패 시 정본과 수렴 방법을 문서화한다. shadow read는 값만 비교하며 운영 부작용이 없어야 한다.
4. **Contract:** 이전 SHA가 필요 없고 새 경로가 검증된 뒤 별도 릴리스에서 옛 컬럼/테이블 삭제. 파괴 단계 전 off-host 백업·격리 복원·forward fix 절차 필수.

**배포 호환 행렬**을 migration마다 작성한다: `old app + old DB`, `old app + expanded DB`, `new app + expanded DB`, `new app + contracted DB`. 앞의 세 조합은 되돌릴 기간 동안 가능해야 한다. Flyway는 DB 자동 rollback이 없으므로 migration 실패 후 `repair`/`clean`을 자동 실행하지 않는다. 비가역 변경은 “이미지 롤백”으로 해결한다고 쓰지 않는다. 기존 data deletion/retention 정책과 개인정보 처리 요구는 적용 전 별도 검토한다.

---

# 파트 D — 인프라·보안·관측 재설계

## D1. 독립 검증 환경과 용량

운영과 **호스트 또는 최소한 프로젝트·볼륨·계정·secret·네트워크가 분리된** 개발/검증 환경을 먼저 만든다(ISSUE-256). 실제 JUMP 수집·실제 발송·실제 OAuth callback이 운영 사용자에게 영향을 주지 않도록 scheduler/발송을 기본 비활성화하고 fake provider/fixture를 사용한다. 운영 DB 사본이 필요하면 비식별화·접근 제한·수명 제한을 먼저 정한다. 같은 OCI의 별도 Compose project만으로 host-level 장애가 격리됐다고 주장하지 않는다.

운영 호스트의 피크 CPU/RSS/디스크와 수집+발송 동시 실행을 계측한 뒤 앱·DB·Redis·관측 서비스의 자원 예산을 정한다. `infra/docker-compose.yml`에 보이는 현재 limit 부재와 ISSUE-101 완료 기록의 차이를 확인한다. 너무 낮은 memory cap은 OOM을 늘릴 수 있으므로 격리 환경에서 부하·복구 실험을 거쳐 점진 도입한다. 볼륨별 사용량, MySQL/Redis 지속성, 업로드 증가, 로그 보존량을 같은 용량 표에 넣는다.

## D2. 비밀·권한·네트워크

| 대상 | 설계/검증 과제 |
| --- | --- |
| MySQL | 앱 DML 계정과 one-shot migrator DDL 계정 분리 후보를 검증한다. 현재 둘 다 root인 이유(운영 단순화)와 침해 범위를 명시한다. 계정 전환 시 백업/복원/초기 설치 권한까지 시험한다. |
| Docker/SSH | `ubuntu` Docker socket 권한은 사실상 호스트 관리자 권한이다. GitHub secret·deploy key·known_hosts·서버 `.env`·Firebase 파일의 소유/회전/폐기 경로를 확인한다. 현재 CD는 host key를 variable에 고정한다. |
| 접근 경로 | MySQL/Redis host port 비공개, 앱·Actuator loopback 및 NPM/런타임 네트워크 연결, 관리자 API 인증, outbound egress를 실제 Compose/NPM 설정으로 검증한다. NPM·인증서·DuckDNS는 저장소 밖 운영 자산으로 재구축 자료를 별도 수집한다. |
| 인증/API | OAuth redirect allowlist, secure/HttpOnly/SameSite cookie, CSRF/CORS, refresh 회전, 사용자·관리자 권한, 업로드 크기/타입/다운로드 권한을 계약 시험한다. 불필요한 토큰·PII 로깅을 막는다. |

권한 분리, secret store, edge 교체를 한 번에 하지 않는다. 먼저 현재 권한·위협·운영 부담을 기록하고 침해 영향 축소와 장애시 복구 가능성을 함께 비교한다.

## D3. 백업·재해 복구

**이 파트는 구조 변경 전 P0 게이트다.** OCI 로컬 dump와 named volume은 host-loss 보호가 아니다. `docs/disaster-recovery-policy.md`의 MacBook off-host 절차가 실제 예약 실행되는지, 최근 성공 시각·checksum·보관 기간·암호화/접근권한을 운영자 환경에서 확인한다. `infra/scripts/backup-server-export.sh`, `backup-to-macbook.sh`, `verify-mac-restore.sh`의 실행 결과를 기록한다. 이 Mac에서 미확인이라는 사실을 모든 off-host 백업 부재로 확대하지 않는다.

복구 실험은 빈 격리 환경에서 **MySQL dump + 첨부 archive + 필요한 설정/secret 목록 + 앱 이미지/Compose**로 수행한다. `users/courses/subscriptions/feedback_attachments` row, 실제 파일 bytes, 권한, OAuth/알림 비활성 상태에서의 readiness를 확인한다. Redis AOF를 복원해야 하는지 또는 세션 재로그인·토큰 철회 정책으로 대체할지 결정한다. 백업 시 DB row와 파일 snapshot 시점 차이로 생기는 고아/누락을 검사한다. 결과로 실제 RPO/RTO를 측정하고 목표가 24시간/30분이면 충족 여부를 증명한다. 불충족이면 백업 빈도, off-host 대상, 보존, 자동 경보를 수정한 뒤 다시 훈련한다. Host-loss뿐 아니라 잘못된 migration/운영자 삭제도 복구 시나리오에 포함한다.

## D4. 관측과 경보

현재 프로필의 Prometheus/Loki/Alloy/Grafana는 `infra/docker-compose.yml`에 있다. Alertmanager는 현재 Compose에 없다. `test-observability-smoke.sh` 실패는 배포를 멈추지 않는 warning이므로 이를 장애 탐지 보장으로 쓰지 않는다. 실제 가동·데이터 보존·디스크 소비·대시보드 사용 빈도를 확인하고 다음의 **최소 진단 신호**를 먼저 확보한다.

- API: route group별 요청량·오류·지연, OAuth/refresh 실패율, 관리자 접근 실패. 경로 변수·사용자 식별자를 label로 넣지 않는다.
- 수집: 마지막 성공 시각, 전체/부분 실패, upstream latency, 처리 강의 수, 변화 수, 동시 실행 여부.
- 알림: outbox 대기 수/최고 age, claim/lease 연장/만료, 첫 시도 지연, provider별 성공·timeout·실패, DLQ 및 replay.
- 저장/배포: MySQL·Redis readiness, 디스크·볼륨 증가, 백업 성공/나이, migration 결과, 앱 readiness, 배포 SHA/중단 시간.

경보는 “누가 어떤 조치를 할 수 있는가”가 있는 신호에만 붙인다. 기존 Slack/외부 uptime monitor의 실제 동작을 확인하고, 관측 스택을 유지할지 단순화할지는 **탐지 시간·진단 시간·월 CPU/메모리/디스크·운영 작업량**으로 비교한다. 단일 호스트 자체가 죽으면 그 호스트의 Grafana도 죽으므로 호스트 밖 탐지 경로가 필요하다.

## D5. 운영 토폴로지 대안과 선택 조건

후보를 실제 비용·복구 실험 없이 “더 안전하다/싸다”로 판정하지 않는다. 동일한 피크 부하, host-loss, 잘못된 migration, 1인 운영 시나리오로 비교한다.

| 후보 | 장애/데이터 경계 | 배포·운영 비용 | 재검토 트리거 |
| --- | --- | --- | --- |
| **현 단일 OCI + 검증된 off-host 백업** | 앱·DB·Redis가 함께 죽을 수 있으나 복구 절차를 단순하게 유지 | Compose/SSH 유지; 백업·복구·호스트 재구축을 운영자가 소유 | 실제 RPO/RTO를 만족하고 피크 자원 여유가 있으면 우선 유지. |
| **DB 또는 첨부만 관리형/외부 저장소로 분리** | 호스트 소실에서 해당 데이터의 장애 영역을 분리 | 네트워크·계정·비용·백업/복구 경로와 데이터 이관이 추가 | 단일 호스트의 복구 목표 불충족 또는 첨부/DB 증가가 계측되면 비교. |
| **별도 worker/호스트** | 크롤러·발송과 API 자원을 격리 가능 | 중복 scheduler 방지, DB/queue 일관성, 추가 배포·관측 필요 | 앱 resource budget만으로 피크 지연을 지킬 수 없을 때. |
| **다중 서비스/오케스트레이터** | 독립 확장·재시작 가능, DB가 여전히 공유면 장애 격리는 제한됨 | 계약·네트워크·운영 인지 비용이 가장 큼 | 독립 배포/확장의 실측 요구와 이를 운영할 능력이 확보될 때만. |

관측 역시 `Prometheus/Loki/Alloy/Grafana 유지`, `필수 Actuator+Docker 로그+외부 탐지로 축소`, `관리형 관측으로 이전`을 같은 탐지·진단·보존·자원 기준으로 비교한다. 현 구성의 서비스 수 자체를 근거로 제거하지 않으며, 과거 ISSUE-099의 30일/Alertmanager를 현행 보장으로 가정하지 않는다.

---

# 파트 E — CI/CD·릴리스 재설계

## E1. 빌드와 계약 게이트

현재 PR CI는 경로별 job을 실행하고 서버는 `clean check migrationTest bootJar` 및 필요한 경우 ARM64 이미지 빌드를 확인한다 (`.github/workflows/ci.yml`). 이를 기반으로 다음 순서로 게이트를 구성한다: 정적/아키텍처 의존 규칙 → 단위·contract → MySQL/Redis 통합/마이그레이션 → 이미지/Compose policy → 격리 환경 smoke. 모든 PR에 장시간 운영 부하 시험을 얹지 않고, 고위험 schema·알림·배포 변경에서만 해당 실험을 추가한다. 테스트 결과와 실제 DB 버전·SHA를 남긴다.

API 계약은 웹에서 쓰는 경로를 우선 고정한다. OpenAPI 또는 스냅샷은 생성 방법보다 **CI가 호환성 차이를 감지하고 사람이 의미를 판단할 수 있는지**가 중요하다. 허용 변경(선택 필드 추가)과 breaking 변경(필수값, 상태코드, 권한, 쿠키, 삭제)을 분류한다. 프론트 코드는 이 계획의 CI 개선 대상이 아니다.

## E2. 배포 순서와 실패 지점

현재의 직렬 배포·SHA 이미지·host key pinning·Flyway one-shot·readiness를 기준선으로 보존한다. 배포 재설계는 단계마다 이전 정상 서비스가 살아 있는지/멈추는지 표시한다.

| 단계 | 게이트와 증거 | 실패 시 행동 |
| --- | --- | --- |
| 빌드/푸시·staging | SHA, 이미지 digest/아키텍처, 설정 누락·secret 비출력, Compose 검증 | 운영 앱 미변경. |
| 기반 서비스·사전 점검 | DB/Redis health, 디스크 여유, 최근 복구 가능한 백업, migration 호환 행렬 | 앱 중단 전 실패. 원인 해결 후 동일 SHA 재시도. |
| expand migration | 예상 DDL lock/시간, Flyway 버전·checksum, old app 호환 | 실패 시 자동 repair/clean 금지. DB 상태 조사 후 forward fix 또는 백업 복원 결정. |
| 앱 전환 | 새 앱 시작/readiness 및 API/OAuth/핵심 경로 smoke, outbox/수집 scheduler 중복 방지 | schema가 old app과 호환될 때만 이전 SHA 재배포. 그렇지 않으면 forward fix/격리 복원. |
| 관측·안정화 | 검색 오류·수집 성공·outbox lag·DLQ·자원·백업 상태를 정한 관찰창에 확인 | 되돌림 기준 초과 시 운영 중지/이전 SHA/기능 비활성 등 해당 변경의 회복 수단 실행. |

현재 스크립트는 기존 앱을 멈춘 뒤 migrate한다. 위 표의 “expand migration”을 앱 중단 전 실행할지는 **old app 호환성과 운영 실험 후** 변경할 제안이지 현재 구현 설명이 아니다. blue-green/무중단 배포는 NPM, 세션, scheduler, DB, 호스트 자원까지 검증하지 않고 선언하지 않는다. 관측 smoke는 현재 advisory이므로 백업/핵심 서비스 게이트와 구분한다.

## E3. 롤백·재구축 계약

릴리스마다 **앱 SHA, DB migration 버전, 호환 가능한 이전 SHA, 필요한 secret/config 버전, 비활성화할 scheduler/worker, 예상 사용자 영향**을 기록한다. 앱 이미지 롤백은 schema 호환성 검사 후에만 한다. migration·데이터 오류는 별도 forward migration 또는 격리 복원 후 전환을 사용한다. outbox가 이미 provider에 전송한 알림은 DB 복원만으로 취소할 수 없으므로 replay/중복 위험을 별도로 평가한다.

호스트 재구축 리허설에는 OCI 인스턴스·Docker/Compose·네트워크/NPM/인증서·GHCR 접근·secret 설치·DB/첨부 복원·Flyway 상태·readiness·OAuth callback·외부 발송 비활성/재개를 포함한다. `.github/workflows/cd.yml`과 [배포 런북](operations/deployment.md)의 서로 다른 책임을 명확히 하고, 재구축에 필요한 저장소 밖 수동 설정을 빠짐없이 목록화한다. 자동화는 반복 실패가 확인된 단계부터 추가한다.

---

# 파트 F — 실행 순서와 독립 완료 게이트

날짜·소요 기간은 운영 데이터/가용 시간 없이 추정하지 않는다. 각 단계는 선행 게이트를 만족해야 시작한다. 작업 ID는 계획의 추적 단위일 뿐 새로운 이슈 운영 체계를 요구하지 않는다.

| 단계 | 작업 묶음 / 선행 | 종료 증거 | 되돌림/중단 기준 |
| --- | --- | --- | --- |
| **0. 사실 확인** | 인벤토리 검산, 운영 Compose override/실제 서비스·백업·secret 위치·NPM, 운영 수치 수집. | 현 HEAD/운영 차이 목록, 신뢰도와 날짜, API·테이블·볼륨·외부 의존 지도. | 운영 접근 불가 항목은 `미확인` 유지; 그 항목에 의존한 비가역 결정 중단. |
| **1. 데이터 보호** | off-host DB+첨부 백업·격리 복원, RPO/RTO 합의. 단계 0 선행. | checksum·restore log, 주요 row/bytes 검사, 실측 RPO/RTO와 운영자 runbook. | 실제 복구 실패 시 스키마 삭제·스토리지 이전·대규모 배포 중단. |
| **2. 계약·코드 구조 기준선** | API/인증/수집/알림/파일 characterization과 실패 주입, 도메인 import·read/write 지도, 대표 흐름 추적, 중복·미사용·복잡 흐름 후보 조사. 단계 0 선행, 운영 데이터 필요 시 1 선행. | contract/test 기준선, 흐름 추적도, 후보별 코드 근거·소비자 확인·확신도·영향·다음 단계, 측정 자료. | 중요 흐름을 재현할 수 없으면 그 흐름 구조 변경 중단; 삭제 후보 소비자가 불명확하면 삭제 보류. |
| **3. 설계 결정** | A/B/C 대안 시험, 도메인 소유권·API/DB/Redis 경계, 확인된 코드 부채 우선순위, 복구/비용 결정 ADR. 1·2 선행. | 선택 이유·포기한 것·재검토 조건, 호환 행렬, 단계별 작업 범위와 변경하지 않을 복잡성의 근거. | 사용량/실패/코드 의존 근거가 없으면 더 작은 변경으로 제한하고 비가역 선택 보류. |
| **4. 독립 검증 환경** | ISSUE-256에 대응하는 별도 데이터·secret·provider stub, 배포·복구 smoke. 1·3 선행. | 운영 영향 없는 기동/health/migration/복구 결과. | 운영 OAuth/크롤러/알림과 연결 가능하면 실험 중단. |
| **5. 고위험 정합성** | ISSUE-246 탈퇴, ISSUE-249 lease, 수집 원자성, 파일 orphan 경계. 2·4 선행. | 실패 주입/동시성 테스트, API 동일성, backlog/오류 관찰. | 사용자 계약 회귀, 발송 중복/유실 증가, 데이터 불일치 시 이전 호환 SHA 또는 forward fix. |
| **6. 바텀업 도메인 코드 리팩토링·이전** | 단계 2·3의 상위 finding에서 작은 기능 조각을 고른다. 규칙/모델→저장 경계→application 흐름→controller·scheduler·도메인 간 통합 순으로 붙이며 한 소유권/실패 경계씩 진행한다. 도메인 순서는 감사 근거와 위험도에 따라 정하고 미리 고정하지 않는다. 3·5 선행. | 각 조각의 불변조건 테스트, persistence/API 계약, 전후 호출 흐름, finding 해결 근거, 성능 비교·운영 관찰. 통합 후 동일 contract suite 통과. | 여러 도메인·schema·API 의미를 한 번에 바꾸거나 소유자가 불명확해지면 중단하고 조각을 더 작게 나눈다. 모듈 분리는 이익이 확인된 경우에만 한다. |
| **7. 인프라/CD 정리** | DB 계정, resource budget, backup alert, 관측 스택, 배포 재구축 개선을 각각 별도 릴리스로. 1·4 선행. | 보안/부하/배포/복구 리허설, runbook·ADR 최신화. | 제한으로 OOM, 권한으로 배포/백업 실패, 경보 미도달 시 해당 설정 복원. |
| **8. 제거·최종 검증** | 사용 증명 없는 옛 경로/테이블/의존 제거; contract migration 별도 릴리스. 6·7 안정화 후. | 전 경로 회귀, schema 호환 기간 종료 승인, 최종 복구 훈련·문서 정합성. | 외부 소비자/데이터 참조 미확인 시 삭제 보류. |

단계 1·2는 일부 병행 가능하지만, 복구가 증명되기 전 운영 데이터의 파괴적 실험은 하지 않는다. 단계 5 안에서도 탈퇴, lease, 수집, 파일은 각각 독립 배포한다. 이 계획은 ISSUE-258의 실행 내용을 구체화하지만 이슈 완료를 선언하지 않는다.

## 단계별 작업 결과물의 최소 형식

각 설계·제품 계약·데이터·보안·인프라·배포 관련 결정은 `docs/decisions/`에 Markdown으로 남긴다. 작은 결정은 템플릿을 간단히 채우고, 되돌리기 어렵거나 여러 영역에 영향이 큰 결정은 대안·전환·복구 조건까지 자세한 ADR로 기록한다. 실제 운영 로그와 검증 날짜를 기록하며 기존 결정의 이력은 덮어쓰지 않는다. 변경 결과와 측정 로그는 해당 결정 기록에서 링크한다.

---

# 파트 G — 미결 결정, 위험, 최종 승인 기준

## G1. 먼저 답할 질문과 결정 트리거

| 질문 | 조사 방법 | 답에 따른 설계 변화 |
| --- | --- | --- |
| 허용 가능한 데이터 손실·중단은 얼마인가? | 운영자와 RPO/RTO 확정, 최신 백업 복구 측정. | off-host 빈도/대상, 단일 호스트 수용 여부, migration 방식. |
| 현재 백업과 MacBook 수집이 실제 도는가? | 각 호스트 timer/cron·파일·checksum·복원 결과 확인. | 이행 중단 게이트 및 백업 자동화 범위. |
| 외부 API 소비자와 주요 기능 사용량은? | API access 집계(PII 제거), Vercel/관리자 사용 정보. | API 호환 기간, 중복 endpoint 제거 가능성, 인증 방식. |
| 크롤링·알림 피크가 어디인가? | 두 대표 기간의 지연/DB write/backlog/host 사용량; 실패 주입. | ISSUE-212 좌석 분리, worker lease/fan-out, 별도 job/host 필요성. |
| 관측 스택과 독립 검증 서버의 운영 비용은? | 가동 상태, CPU/RSS/디스크·장애 진단 시간·월 비용 비교. | 현 Compose 유지/축소/관리형 서비스 선택. |
| 인증 상태 층을 줄일 수 있는가? | 쿠키/세션/JWT/Redis 요청 trace와 장애 재현. | JWT/세션 유지·통합·제거 결정. |

## G2. 위험 등록부

등급은 **잠정 영향도**다. 발생 가능성은 운영 자료 부족으로 수치화하지 않는다.

| 위험 | 영향 | 대응/상태 |
| --- | --- | --- |
| host-loss 때 DB·첨부 동시 복구 불가 | 매우 큼 | **차단:** 1단계 복구 시험 전 비가역 변경 금지. |
| Flyway 적용 후 이전 SHA가 기동 불가 | 큼 | **완화:** expand/migrate/contract, 호환 행렬, 사전 백업·forward fix. |
| DB 탈퇴 commit과 Redis 철회 불일치 | 큼 | **미해결:** ISSUE-246 실패 주입 후 수렴 설계. |
| lease 초과/부분 fan-out의 중복 알림 | 큼 | **미해결:** ISSUE-249 다중 worker 테스트, target별 멱등성·provider 보장 분리. |
| 단일 호스트 자원 경합 및 관측 자체 손실 | 큼 | **완화 예정:** 피크 계측, 자원 예산, 외부 uptime/백업 경보. |
| root DB/ubuntu Docker 권한 노출 | 큼 | **수용 여부 결정 필요:** 계정 분리와 운영 비용 비교, secret/host key 회전 점검. |
| API/쿠키 계약 파손으로 웹 로그인·알림 기능 불능 | 큼 | **완화:** 웹 무수정 contract suite와 단계적 API 호환. |
| 불필요한 모듈·큐·서비스 추가로 인지부채 증가 | 중간~큼 | **억제:** 변경 시나리오/운영 측정으로만 복잡성 추가 승인. |

## G3. 설계 승인과 완료 판정

설계를 확정하려면 **(a)** 실제 운영/복구 자료 또는 명시적 미확인, **(b)** 동일 기준의 대안 비교, **(c)** 도메인·데이터·실패 경계, **(d)** API/스키마 호환 및 전환 순서, **(e)** 검증과 되돌림 수단이 있어야 한다. 하나라도 없으면 결정은 “보류/조건부”로 기록한다. 특히 새 DB, 메시지 브로커, Kubernetes, 마이크로서비스, 인증 전면 교체는 별도 비용·복구 실험 없이는 채택하지 않는다.

전체 작업 완료는 다음 증거로만 선언한다: 핵심 계약 테스트와 MySQL migration suite 통과, 격리 환경의 실패 주입/복구 훈련, 운영 관찰창에서 기존 대비 지연·오류·중복·자원 회귀 없음, 실제 백업/경보 확인, 최신 `docs/operations/deployment.md`와 ADR, 불필요한 코드/설정 제거의 소비자 검증. 목표 수치와 관찰창은 0~3단계에서 실측과 제품 허용치로 확정한다.

**이 문서 작성 시점에 구현·프론트엔드·운영 환경은 변경하지 않았다.** 다음 구체 작업은 0단계 운영 사실 확인과 1단계 복구 검증이다. 두 결과가 설계의 우선순위를 바꿀 수 있다.
