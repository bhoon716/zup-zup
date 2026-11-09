# 로깅·모니터링 보강 결과

기준: 2026-11-09 UTC, 로컬 기준 커밋 `32d5db21` 이후 변경. 이 문서는 코드와 로컬 검증 결과다. 운영 배포·실제 알림 수신·자동 백업 활성화 완료를 의미하지 않는다.

## 구현한 것

- DB 실패 기록과 Slack 요청을 분리했다. 트랜잭션 시작/조회/저장/commit 실패가 알림 요청을 막지 않으며, 원래 수집 오류를 다른 오류로 덮어쓰지 않는다.
- 수집, 알림 작업자, 상태 감시 스케줄러를 분리했다. DB 조회 실패는 unknown/실패로 표시하고, 마지막 성공 시각을 노출해 감시 자체의 정지도 감지할 수 있다.
- `notification.outbox.queue.depth`는 DB의 실제 READY 건수다. 미래 재시도와 활성 선점은 제외하고 만료된 선점은 포함한다. 처리 중 건수는 `notification.outbox.in.flight`로 분리했다. 집계는 주기 작업이 수행하며 스크레이프 중 DB를 조회하지 않는다.
- Slack은 한 개 비동기 작업자와 최대 64개 대기 작업을 사용한다. 256개 중복 키에는 실패 위치를 포함한다. 성공/실패/억제/거부/비활성을 제한된 label로 계측하고, 종료 시 최대 5초 동안 배출한다. 메모리 큐이며 crash 내구성은 없다. 결과가 불명확한 HTTP 요청을 무조건 재전송하지 않는다.
- 브라우저 오류에 검증된 서버 correlation ID와 오류 코드를 연결했다. 서버와 동일한 최대 64자/문자 규칙을 적용하고, 잘못된 값은 자르거나 변환하지 않고 제외한다. 원문 응답·요청·이메일·URL을 추가하지 않는다.
- 경로/쿼리 타입 오류, 누락 입력, 메서드 인자 검증 오류는 400이다. 실제 구현 오류와 반환값 검증 실패는 500/Slack 경로로 유지한다.
- 로그 작성은 의미있는 전용 메서드로 분리했다. `event`, `stage`, `originalType`, `recordingType`, `alertType` 등은 메시지 파싱이 필요 없는 독립 JSON 필드다. 실제 encoder 출력과 민감 원문 미출력을 검증한다.
- 기본/운영 관리자 감사 보존값은 0(자동 삭제 중지)이다. 명시적으로 유한 기간을 선택한 환경값은 여전히 우선하므로 운영값 확인이 필요하다.
- 최소 감사 사실을 SQL 압축 파일, manifest와 SHA-256으로 보관하고, 감사 사본은 Mac의 일반 백업 회전에서 제외한다. 임의 메타데이터·답글 본문은 아카이브하지 않는다. 두 백업 스크립트는 release에 함께 포함한다.
- Prometheus의 앱/관측 구성요소 수집과 기본 11개 경보 판정 규칙, 독립 시간 기반 신선도 판정을 추가했다. Alloy는 `sugang-helper` 프로젝트만 수집한다. JSON 수집과 아카이브/복구 계약 검사는 CI에 연결했다.
- 호스트 저장 공간/inode 점검, 자동 실행 템플릿과 상태 수집·경보를 추가했다. 운영 설치·활성화는 하지 않았다.

## 추가 운영 보강

- **알림 경로:** opt-in Alertmanager → 승인된 Slack webhook 경로와 합성 FIRING/RESOLVED 검사 도구를 준비했다. 기본 discovery는 비어 있다. 기존 webhook 재사용 승인에 따라 저장소·release 밖에 소유자 전용 로컬 준비 파일을 만들었지만, 운영 runtime에 반영하거나 실제 전송하지 않았다. 준비 파일은 비활성이며 이미지 digest도 아직 지정하지 않았다. 공식 이미지 digest 형식, release 밖 경로와 내부 target을 사전 검사한다. 활성화된 runtime 설정은 다음 배포에도 유지하고 설정 파일 교체 시 Alertmanager를 재생성한다. API 수락과 실제 Slack 수신은 구분한다.
- **호스트 자동 실행:** 저장 공간·중요 사건 로그는 5분, DB·감사·업로드는 일일 systemd 템플릿으로 준비했다. 고정 label의 원자적 textfile 지표를 기존 Alloy가 읽으며, 실패·장기 실행·누락을 감지하는 경보 8개를 추가했다. CD는 파일만 준비하고 timer를 설치하거나 시작하지 않는다. root runner는 점검·지표만 관리하며 export 자식은 기존 ubuntu 계정으로 실행한다. 기존 root-private 기록의 권한을 자동 변경하지 않는다.
- **중요 사건 로그:** 현재 app 컨테이너의 최근 10분 중 허용 event·시각·종류·검증된 수집 실행 ID 등 최소 사실만 압축/manifest/체크섬으로 보관한다. 원문 message·stack·URL·본문·사용자 ID는 제외한다. Mac의 events 사본은 일반 백업 회전에서 제외한다. 중복 창·로그 순환·컨테이너 교체·10분 초과 중단의 누락 가능성을 명시했으며 완전한 영구 원장은 아니다.
- **백업·복구:** 전체 DB·업로드·manifest를 완성 디렉터리로 원자 게시하고 기본 업로드 읽기 실패는 DB-only 성공으로 바꾸지 않는다. 업로드는 app 컨테이너에서 읽어 host Docker data-root 권한 확장을 피한다. Mac에서도 전체 묶음을 원자 승격하고 묶음 전체가 만료된 뒤 기존 유한 회전 정책을 적용한다. 감사/events는 회전하지 않는다. 격리 복구 검사는 현재 37개 테이블·Flyway·각 테이블 읽기·감사 schema/정확한 행 수와 첨부 무결성을 확인한다. 원본 snapshot의 모든 행 일치나 앱 기능 복구를 보증하지 않는다.

독립 검토에서 새 연결의 소유권 불일치, 묶음 회전과 반복 동기화 충돌, Alertmanager 설정 미반영을 찾아 수정하고 회귀 검사를 추가했다. 기존 데이터·백업·서비스를 실제로 삭제/재기동하지 않았다.

## Grafana 관리

대시보드는 사용자가 Grafana UI에서 관리한다. 이번 변경에서 기존 dashboard JSON/provisioning은 수정하지 않았고 새 dashboard도 추가하지 않았다. 기존 패널의 데이터 부재를 0으로 대체하는 쿼리와 새 지표 표시 여부는 UI에서 검토해야 한다. 운영 Grafana에는 접근하거나 변경하지 않았다.

## 이전 구현 단계의 검증

아래는 앞선 구현 단계의 실행 기록이다. 2026-11-09 최종 재검증과 후속 백업 확인은 다음 절에 따로 기록한다.

- 추가 운영 보강의 최종 오프라인 검사: Python 70건(runner 23, 중요 사건 14, 저장 공간 9, 알림 전송/구성 13, 설정 사전검사 11), 감사/전체 export·Mac 동기화·복구·CD·환경 사전검사·release 동기화 계약, 13개 격리 배포 시나리오를 통과했다. 모든 Docker 명령은 이 검사에서 stub이며 실제 Docker/SQL/외부 수신의 증거가 아니다. shell/Python 문법, YAML 11파일 구문과 diff 검사를 통과했다. 새 호스트 경보의 39개 promtool fixture·Alloy textfile runtime 검사·Alertmanager amtool 검사는 준비했으나 실행하지 않았다. 이전 앱 단계의 전체 검사와 이번 infra 집중 검사를 구분한다.

- 당시 서버 `check bootJar modulithE2e` 통과. 일반 suite 613건과 Modulith E2E 14건, 실패/오류/skip 0이며 main/test Checkstyle·PMD도 통과했다. 로컬 JVM의 Mockito self-attach 문제는 테스트 시작 시 agent를 지정해 검증했으며 운영 코드는 변경하지 않았다.
- 브라우저 전체 Vitest 87파일/417건, TypeScript `--noEmit --incremental false`, 전체 ESLint를 통과했다.
- Prometheus 경보 44개 합성 시나리오, 고정 이미지 설정 검증, 격리 JSON 로그 파이프라인을 본 작업에서 통과했다. 이후 최종 Docker 재실행은 socket 접근 정책 제한으로 실행하지 못했다. 설정/규칙의 실행 결과와 이 최종 재실행 제한을 구분해야 한다.
- 최소 감사 아카이브는 합성 한 행으로 격리 MySQL 복구를 검증했다. shell 계약 테스트는 별도로 Docker를 stub하므로 실제 복구 검증의 대체 증거가 아니다.
- CD/배포 계약, 11개 격리 배포 시나리오, 환경 사전검사, release 동기화, 감사 export/sync/restore 계약과 9개 저장 공간 fixture 테스트를 통과했다.
- 웹 production build는 dependency registry 네트워크 정책으로 중단되었다. 단위검사/typecheck/lint 통과를 production bundle 통과로 표현하지 않는다.
- 실제 운영 DB/대학 API/Slack, 실제 디스크 상태와 대규모 집계 성능은 검증하지 않았다. migration/session/manual/performance 전용 suite는 이번 변경의 검증에 포함하지 않았다. 당시 기본 Mac 백업 경로에서는 실제 보관본을 확보하지 못했다. 이후 다른 로컬 보관 위치에서 확인한 DB 사본은 아래 최신 기록에 구분한다. 이번 추가 단계에서 기존 SSH/공개 HTTPS·Docker 접근 차단을 재시도하거나 우회하지 않았다.

## 최종 로컬 검증과 백업 확인 — 2026-11-09

- 최종 코드의 `./gradlew --offline check bootJar`는 exit 0이다. JUnit XML 134파일·616건, 실패·오류·skip 0이며 Checkstyle·PMD도 통과했다. 이 실행에는 Docker 기반 Modulith E2E·MySQL migration·Redis 전용 검사를 포함하지 않았다.
- 웹 전체 ESLint, TypeScript `--noEmit`, Vitest 87파일·417건을 통과했다. 첫 전체 실행에서 변경하지 않은 lint 의존성 호환 테스트 1개가 5초 제한을 넘겼다. 코드나 timeout을 바꾸지 않고 단독 2건과 전체 `--maxWorkers=2`를 다시 실행해 통과했다. production build 완료를 뜻하지 않는다.
- Python 70건과 shell 구문, CD·환경·release layout·백업 export/sync/restore 계약 및 13개 격리 배포 시나리오를 다시 통과했다. 실제 Docker 명령은 stub이다. Slack Java 21건, alert/settings Python 24건은 전체 검증과 중복되는 집중 검사이며 별도로 더하지 않는다.
- 한국어 Slack 안내는 현재 상태·확인된 영향 범위·감지 시각·첫 대응·안전한 추적 검색식을 담는다. 단건 실패로 전체 장애·복구·긴급도를 단정하지 않는다. Alertmanager의 발생·해제 템플릿은 Go template 66개 조합을 검사했으며 실제 Slack 수신은 확인하지 않았다.
- 여섯 독립 관점의 최종 코드 리뷰에서 추가 확정 결함은 없었다. diff 공백 및 secret 패턴 검사도 통과했다. 이것은 전체 CI·운영 준비 완료 판정이 아니다.
- 11월 5일 생성된 로컬 DB dump 사본(10,745,453 bytes)을 별도 보관 위치에서 찾았고 SHA-256 대조와 `gzip -t`가 통과했다. DB 사본과 checksum만 확인했으며 업로드·로그 묶음이나 실제 MySQL 복원 성공은 확인하지 않았다. 백업 내용과 credential은 출력하지 않았다.
- 사용자가 제공한 서버 상태 출력에는 기존 매주 월요일 04시 백업과 11월 9일 04시 최신 DB dump(10,747,318 bytes) 및 checksum 파일이 있었다. 신규 세 systemd service는 not-found, matching timer는 0이었다. 파일 존재는 checksum 검증·복원 성공이 아니며 서버 cron 원문·현재 실행 정책은 독립 확인하지 않았다.
- 운영 SSH와 Docker의 기존 접근 거부는 재시도·우회하지 않았다. 운영 배포·기존 스케줄 정리·새 timer 활성화·실제 Slack 수신·실백업 복원은 미완료다. 기존 주간 백업을 중단하거나 삭제하지 않았다.

## 운영에서 마무리할 항목

1. 변경 검토 후 승인된 절차로 배포하고 실제 배포 SHA·metrics·규칙 평가를 확인한다.
2. 외부 경보 수신자/전달 경로와 별도 장애 도메인의 uptime 감시를 확인하고, 장애·복구 테스트 알림을 수신한다. 같은 호스트의 Prometheus는 호스트 전체 중단을 외부에서 감지할 수 없다.
3. 실제 `APP_ADMIN_AUDIT_RETENTION_DAYS=0`을 확인한다. 기존 `backup-db-local.sh` timer는 자동 변경하지 않는다. 새 timer 템플릿을 승인 설치하기 전에 기존 주간 작업과 관계·권한·오프호스트 주기·용량/회전 정책을 확정해야 한다. 서버 새 full export에는 자동 pruning이 없으며 ADR의 최신 세 개 정책이 저절로 적용되지 않는다.
4. 마지막 아카이브/체크섬/Mac 외부 사본과 실제 데이터 복구를 확인한다. 저장 위치·암호화·장기 비용과 여유 공간을 결정한다. 현재 사본은 WORM/서명/암호화 보장이 없고, 같은 호스트만으로는 서버 손실을 보호하지 못한다.
5. 실제 Docker data-root와 ubuntu의 기존 Docker/백업 접근 권한을 확인한 뒤 timer와 경보 경로를 활성화하고 실패·복구·실행 누락을 실제 수신한다. 새로운 root-private 기록을 만들거나 기존 기록에 재귀 chown을 자동 적용하지 않는다.

일반 진단 로그는 Loki 30일 등 기존 유한 정책을 유지한다. 모든 로그의 영구 보존이 아니다. 별도 감사 아카이브는 관리자 조치의 최소 사실만 포함하며, 모든 배포 이력·계정 생애주기·알림 발송 이력의 영구 원장을 뜻하지 않는다. DB가 고장 나 저장하지 못한 사건이나 Slack 큐가 process 종료로 유실된 사건의 영구 보관도 보장하지 않는다.

관련 절차: [중요 감사 기록](important-record-retention.md), [중요 사건 로그](important-event-backup.md), [호스트 자동 실행](host-job-automation.md), [복구 검증](restore-readiness.md), [알림 수신](../../infra/alertmanager/README.md), [Prometheus 경보](../../infra/prometheus/README.md), [로깅 세부 정책](../logging-and-alerts.md).
