# 로그와 Slack 오류 안내

기준: `codex/simplify-existing-20261026`, 2026-10-26 사용자 승인. 새 관리자 로그 화면·감사 기능·외부 전달 경로를 추가하지 않는다.

## 사람이 읽는 메시지와 검색 키

서버 소유 로그의 설명을 한국어로 바꾸고 기존 이벤트 태그·오류 코드·분류·단계·타입·내부 ID는 검색 가능한 키로 유지한다. Logback 패턴은 기존 `correlationId`에 `crawlerRunId`를 더해 HTTP와 수집 실행을 추적한다. 실제 환경·correlation/run ID로 Grafana에서 조회하되 실제 대시보드 URL이나 운영 설정은 이 변경에 포함하지 않는다.

| 레벨 | 기록 위치와 의미 |
| --- | --- |
| DEBUG | 정상 HTTP·health, 수집 단계/묶음 완료·재시도 대기, 제공자 시도/성공/개별 실패, 빈자리 감지, 반응·관심 강의 변경 |
| INFO | 수집 전체 시작/완료, 구독/계정/시간표 변경, 보존 기간 정리 결과, 선점이 바뀐 정산 생략 |
| WARN | API 4xx·인증/인가 거절, 실제 발송 재시도 예약, 상위 API 재시도 가능한 실패, health 실패, Slack 전송 실패 |
| ERROR | API 5xx, 수집 최종 실패/신선도 초과/실패 이력 저장 오류, 세션 장애, outbox 처리 예외와 최종 DLQ |

`X-Error-Id`가 있는 HTTP 오류는 핸들러가 이미 기록했으므로 필터 요약을 DEBUG로 남긴다. `/health`는 컨트롤러의 readiness WARN과 HTTP 요약 DEBUG로 기록하고 `/actuator` 요청은 기존처럼 필터 요약에서 제외한다. 식별자가 없는 4xx/5xx는 필터가 WARN/ERROR로 기록한다. 제공자별 시도 로그는 DEBUG, outbox 재시도 예약은 WARN, 최종 DLQ는 ERROR로 구분하여 같은 발송 실패를 제공자와 정산에서 모두 ERROR로 남기지 않는다. 최종 정산에는 예외 타입을 남기며 DLQ·작업 처리 예외에는 안전한 stack trace를 남긴다.

## 원문 보호와 진단 비용

- 예외 메시지·detail·cause/suppressed 메시지는 자격 증명, 외부 응답, SQL 값, 개인정보를 포함할 수 있어 직접 출력하지 않는다.
- `SensitiveDataRedactor.safeStackTrace`가 원래 프레임, 원인/억제 예외 연결과 순환 참조를 복사한다. 각 복사본의 메시지에는 원래 예외의 전체 타입 이름과 원문 생략 표시만 둔다. 원본 예외·API 응답·분류는 변경하지 않는다. Logback에 보이는 복사본 클래스는 `Throwable`이며 실제 타입은 복사 메시지와 별도 `exceptionType`으로 확인한다.
- API 5xx의 기존 1분 stack 샘플링과 Slack category/code/type별 cooldown·최대 256 키 정책은 유지한다. 완전한 사건별 dedup이나 모든 제3자 로그의 정화를 보장하지 않는다.
- 기기 별칭·이모지 입력·교수 이름을 로그 인자에서 제거한다. 이메일은 마스킹된 주소 대신 fingerprint로 기록한다. 기존 토큰/Discord ID fingerprint와 경로 민감 구간 숨김, query 존재 여부만 기록하는 정책을 유지한다. fingerprint와 내부 ID도 접근 통제가 필요한 운영 진단 값이며 익명화 보장은 아니다.
- 웹은 기존 production 오류 메타데이터 정책을 개발 모드에도 적용한다. Axios 오류 객체의 config/headers/body와 원문 메시지를 콘솔로 보내지 않는다. 웹푸시의 직접 경고도 같은 메타데이터 생성기를 사용한다. 기존 analytics 이벤트 구조·중복 억제·기능의 성공/실패 처리는 유지한다. 서버 stack frame은 보존하며, 브라우저 개발 콘솔의 원문 Error 객체 출력은 제거한다.

## Slack에 담는 정보

기존 수집 실패와 API 5xx 알림의 사람이 읽는 내용을 아래처럼 제공한다. 2026-11-08 보강에서 발송을 비동기 작업자로 분리했으며 자세한 동작은 아래 로컬 보강 절에 기록한다. 메시지를 바꾸면서 자동 재시도·복구 기능을 추가하지는 않았다.

```text
[줍줍 오류][PROD] 강의 API 수집 실패
상태: 이번 강의 수집 실패 · 복구 여부 미확인
영향: 최신 강의·여석 반영 지연 가능 · 기존 조회·영향 사용자 범위 미확인
감지: 2026-11-09 14:30:00 (한국 시간)
첫 대응: ① 같은 수집 ID의 실패·재시도 로그 ② 마지막 수집 성공 시각 ③ 다음 수집 결과 확인
오류: C001 · CustomException
진단 단서: 실패 단계=학교 API 접속 준비(JUMP_BOOTSTRAP) 원인 유형=연결 실패(ConnectException) 학교 응답=미확인
재시도: 재시도 가능한 유형 · 실제 예약·실행·성공 여부는 로그 확인
추적: 요청=NONE · 수집=crawl-example
Grafana 검색: {app="jbnu-sugang-helper",job="app"} | json | __error__="" | crawlerRunId="crawl-example"
```

`retryable=true`는 재시도 가능 분류이며 실제 예약·성공이 아니다. API 5xx는 해당 요청 실패까지만 단정한다. 전체 장애·전체 사용자 영향·복구 시간·긴급 등급은 단정하지 않는다. Slack에는 확인된 실패 상태와 첫 대응을 먼저 보여주고 긴 스택은 보내지 않는다. 단건 오류만으로 P1/P2 등급을 매기지 않는다. 수집 ID, 요청 ID 순으로 안전한 JSON 로그 검색식을 제공하며, ID가 없거나 유효하지 않으면 감지 시각 기준 조회를 안내한다. 기존 JSON 로그와 검색용 식별자는 유지하며 상세 스택은 Grafana에서 확인한다. 진단 문자열은 failureStage/failureType/upstreamStatus/retryable만 허용하고 원문 예외·URL·쿠키 등은 첨부하지 않는다. 환경·추적 ID의 제어문자/Slack markup도 허용하지 않는다. webhook 실패는 타입만 WARN으로 남기고 원래 요청에 재전파하지 않는다.

## 검증과 한계

Slack은 mock으로 캡처한 payload만 검사하고 실제 전송하지 않는다. 토큰·쿠키·이메일이 포함된 원인/억제 예외의 렌더링 결과, 원래 stack frame·순환 참조·원본 보존, API stack 샘플링, HTTP 중복 레벨, Slack 진단 allowlist·MDC·cooldown·전송 실패 격리, 웹 콘솔의 안전한 메타데이터를 테스트한다. 최종 결과는 아래 실행 기록에 정리한다.

운영 Grafana 수집·검색과 Slack 실제 렌더링/전송, 외부 제공자의 실제 실패는 검증하지 않았다. 기존 로그 보존/압축 설정과 과거 보존 정책의 차이는 이번 변경의 대상이 아니다. 운영 설정이나 실제 개인정보·자격 증명은 읽거나 변경하지 않았다.

## 최종 실행 기록 — 2026-10-26

- 서버 `clean check migrationTest sessionTest bootJar` 통과. 최종 outbox 원인 타입·안전한 stack 보강 후 `check bootJar`를 다시 통과했다.
- JUnit XML 기준 서버 123 suite·548건, MySQL migration 2건, Redis HTTP 7건 모두 실패·오류·skip 0. PMD main/test 및 Checkstyle main/test 위반 0.
- 웹 Vitest 79파일·316건, TypeScript `--noEmit`, ESLint, production build 통과. Playwright Chromium 9건은 재시도 0으로 통과했다.
- 초기 전체 실행에서 한국어 문구·이메일 fingerprint로 바뀐 계약을 반영하지 않은 기존 assertion 두 곳과 테스트 상수 중복 PMD 한 곳을 발견해 수정했다. 테스트 삭제·skip·실패 은폐 없이 위 전체 검증을 다시 통과했다.
- 연결 복구 후 현재 Java 소스가 컴파일 산출물보다 새롭지 않고 웹 소스가 최종 테스트 이후 수정되지 않았음을 확인했다. 재연결 자체만을 이유로 같은 테스트를 반복하지 않았다.
- 실제 Slack/외부 알림 발송·운영 Grafana 열람·운영 설정/DB 변경·push/merge/deploy는 실행하지 않았다.


## 로컬 보강 — 2026-11-08

이 절은 앞의 2026-10-26 범위 제한 이후 변경을 설명한다. 운영 반영이나 수신 성공을 뜻하지 않는다.

- 실패 이력의 DB 트랜잭션과 Slack 요청을 분리했다. DB 연결·저장·commit 실패에도 기존 Slack 경로를 시도하며 원문 예외는 남기지 않는다. 실패 로그의 stage/originalType/recordingType/alertType은 message 안 문자열뿐 아니라 독립 JSON 필드다.
- Slack 발송은 단일 비동기 작업자와 최대 64개 대기 작업으로 분리했다. 요청/수집 호출자는 webhook 응답을 기다리지 않는다. 256개 중복 제한 키에는 실패 위치를 포함하며 포화 시 기존 키를 지우지 않고 거부를 계측한다. 전송 성공/실패/억제/거부/비활성 카운터를 남기고, 종료 시 최대 5초간 배출한 뒤 취소한 대기 작업을 기록한다. 큐는 메모리 기반이고, 전송 결과가 불명확한 요청의 중복 발송을 피하려고 자동 재시도하지 않는다. crash 내구성이나 알림 도달 보장은 아니다.
- 수집용 기본 스케줄러, 알림 작업자, 상태 감시의 스케줄러를 분리했다. 수집/발송이 길어져도 감시를 실행할 수 있으며, DB 조회가 멈추면 마지막 성공 시각으로 감시 자체의 정지를 판정한다.
- `notification.outbox.queue.depth`는 DB에서 확인한 채널별 **지금 전송 가능한** 대기 건수다. 처리 중인 작업은 `notification.outbox.in.flight`로 분리한다. 미래 재시도와 활성 선점은 READY에 포함하지 않는다.
- 강의 데이터의 마지막 성공 시각, 상태 검사 마지막 성공 시각과 검사 실패 상태를 노출한다. 조회 실패 시 이전 정상값을 재사용하지 않는다. 스크레이프는 DB를 직접 조회하지 않는다.
- 브라우저 client_error/GA 오류 메타데이터에는 검증된 correlation_id/error_code만 추가한다. 잘못된 값은 잘라서 사용하지 않고 버린다. Clarity에는 고카디널리티 추적 값을 추가하지 않는다.
- 경로/쿼리 타입 오류, 누락 입력, 메서드 인자 validation은 400으로 분류한다. 구현 버그와 잘못된 반환값 validation은 500/Slack 경로로 유지한다.
- 이번 변경에서 로그 작성은 의미있는 전용 메서드로 분리했다. 새 실패·보존 이벤트에는 event와 진단값을 독립 JSON 필드로 둔다. 비동기 발송 실패도 호출자에서 검증해 캡처한 추적 ID를 유지한다.
- Grafana 대시보드는 사용자 선택에 따라 UI에서 관리한다. 이번 변경의 Grafana JSON/provisioning diff는 없다.
- [독립 경보 규칙과 한계](../infra/prometheus/README.md), [중요 감사 기록 보존·복구](operations/important-record-retention.md)를 참고한다. 일반 로그/Loki는 유한 보존하고 임시 파일 로그를 영구 기록으로 취급하지 않는다.

새 DB 집계의 실제 운영 쿼리 계획·데이터량·성능은 측정하지 않았다. 조회 timeout을 두었지만 대규모 이력에서의 성능 개선을 주장하지 않는다. 알림 전달, 디스크/호스트 감시, 자동 아카이브 실행과 외부 사본은 운영에서 확인해야 한다.

## 최종 로컬 확인 — 2026-11-09

실무형 한국어 알림과 안전한 추적 검색식을 포함한 최종 서버 `check bootJar` 616건, 웹 Vitest 417건·TypeScript·ESLint를 통과했다. 기존 webhook의 동일 목적 재사용을 승인받아 소유자 전용 로컬 준비 파일을 만들었지만 비활성이며 저장소에 포함하지 않았다. 운영 runtime 변경이나 실제 수신 확인은 하지 않았다. 집중 템플릿 검사와 백업 확인, 미실행 운영 항목은 [보강 결과의 최신 기록](operations/logging-monitoring-hardening.md#최종-로컬-검증과-백업-확인--2026-11-09)에 구분한다.
