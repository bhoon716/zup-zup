# FCM 제거 배포

## 범위

알림 채널은 EMAIL·WEB·DISCORD만 지원한다. FCM 플래그·타입·과거 응답 호환은 제거한다.
표준 웹 푸시에서 사용하는 `fcm.googleapis.com`, VAPID와 서비스 워커는 유지한다.
기존 migration 파일은 수정하지 않고 [V31](../../apps/server/src/main/resources/db/migration/V31__remove_fcm_data_and_settings.sql)을 적용한다.

## 배포 순서

1. 운영 대상 건수와 아래 보존 대상의 상태를 읽기 조회로 확인하고, 데이터·스키마 백업 및 복구 가능성을 확인한다.
2. 새 웹을 배포하고 배포 완료를 확인한다. 구버전 탭은 지원하지 않으며 사용자가 전체 페이지를 새로고침해야 한다. 입력값 보존은 보장하지 않는다.
3. DB·Redis를 실행해 준비 상태를 확인한다. 이번 변경은 DB 엔진 업데이트가 아니므로 DB를 중지하지 않는다.
4. 앱만 중지한다. stop 성공 뒤 Compose의 app 컨테이너 조회와 각 Docker inspect가 성공하고 모두 `State.Running=false`인지 확인한다. 조회·상태 확인 실패나 실행 중인 대상이 있으면 migration을 실행하지 않는다. 조회가 성공하면서 기존 앱 컨테이너가 없는 경우는 허용한다.
5. DB가 실행 중인 상태에서 Flyway migration을 적용한다.
6. 새 앱을 시작하고 readiness와 지원 채널의 정상 동작을 확인한다.

API 버전 표식·전용 갱신 화면·공개 웹 준비 파일은 사용하지 않는다.
웹과 서버의 자동 배포가 동시에 시작되면 새 웹 완료를 확인한 뒤 서버 배포를 진행하도록 운영 순서를 조정한다.
앱 중지부터 새 앱 준비까지 API 중단이 발생한다.

## 삭제·보존 대상

| 대상 | V31 처리 |
| --- | --- |
| 관리자 감사 | FCM delivery에 연결된 `target_type='DELIVERY'` 행만 삭제한다. |
| 발송 작업 | `channel='FCM'`인 모든 상태의 delivery를 삭제한다. |
| 알림 이력 | `channel='FCM'`인 행만 삭제한다. |
| 기기 | `type='FCM'` 행을 삭제하고 device enum을 WEB으로 줄인다. |
| 사용자 | 사용자 행은 보존하며 `users.fcm_enabled` 컬럼을 삭제한다. |
| 공유 outbox | 삭제하지 않는다. FCM 자식이 있는 PROCESSING/DLQ의 상태만 남은 지원 채널 작업에 맞춰 재계산한다. |

outbox는 남은 PENDING/PROCESSING 작업이 있으면 PROCESSING, 활성 작업 없이 DLQ가 있으면 DLQ, 둘 다 없으면 COMPLETED로 정산한다.
아직 materialize되지 않은 PENDING과 관계없는 outbox, 지원 채널의 행, 같은 숫자 ID의 다른 종류 감사 기록은 보존한다.
감사 행은 delivery 삭제 전에 ID와 target type으로 식별하며 JSON 문자열 검색으로 삭제 범위를 넓히지 않는다.

## 실패와 복구

- 실패 단계와 실제 앱·DB 상태를 먼저 확인한다. 앱 중지 확인 전에는 migration을 시작하지 않는다.
- MySQL DDL은 자동 커밋하므로 V31 전체의 자동 rollback을 보장하지 않는다. 중간 실패 시 일부 변경이 이미 적용됐을 수 있다.
- 삭제 후 구버전 JAR만 다시 실행하는 방식으로는 복구할 수 없다. 복구가 필요하면 부분 적용 상태를 확인하고 승인된 데이터·스키마 백업과 그 스키마에 맞는 서버·웹을 기준으로 복구한다.
- 자동 rollback이나 임의의 역방향 SQL·Flyway repair/clean으로 대체하지 않는다. 실제 운영 적용·복구는 사용자 승인 후 진행한다.
- 운영 데이터의 실제 건수·잠금 시간·처리 시간과 백업 복구 가능성은 배포 전에 별도 확인한다.

결정 이력은 [알림 채널 ADR](../decisions/2026-10-01-notification-channel-policy.md)에 남긴다.
