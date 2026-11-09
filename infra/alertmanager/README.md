# 승인된 운영 알림 수신 경로

현재는 **로컬 준비만 완료**했다. 기본 Compose에는 Alertmanager가 없고 Prometheus의 discovery 파일은 `[]`다. 실제 수신처 확인, webhook 제공·재사용 승인, digest 확인, 운영 활성화, 실제 TEST/RESOLVED 수신 검증은 하지 않았다. 앱의 기존 Slack 전송과 Prometheus의 장애 알림은 별도 경로다. 일반 Slack 메시지 전송으로 이 경로의 검증을 대체하지 않는다.

## 구성과 안전 경계

- `docker-compose.alerting.yml`은 승인 후에만 사용하는 추가 Compose 파일이다. Prometheus → Alertmanager → 승인된 Slack incoming webhook으로 전달한다. route는 alertname/service/test_id로 묶고 30초 후 발송, 변경은 1분 간격, 미복구 알림은 4시간마다 반복한다.
- Slack webhook URL은 `api_url_file`로 읽는다. 환경 출력·CLI 인자·git·로그·runbook에 실제 URL을 넣지 않는다. receiver가 지정하는 채널이 실제 수신처이며 임의 channel override는 없다.
- Slack에는 한국어 상태 제목과 고정된 문제 요약, 백업·저장 공간 작업 구분, 합성 점검 ID만 보낸다. 경보 해제는 감지 조건 해제이며 전체 서비스 복구를 보장하지 않는다고 안내한다. 예외 원문·로그·개인정보·전체 label/annotation dump는 없다. 운영 경보 summary에 민감한 값을 넣지 않는다.
- Alertmanager API는 인증 없이도 경보 생성/해제·silence 조작이 가능하다. **127.0.0.1만** 바인딩하고 프록시·공인 방화벽에 노출하지 않는다. 동일 호스트 사용자와 내부 Docker 네트워크는 신뢰 경계다. 별도 cluster listener는 비활성이다.
- 내부 runtime 네트워크에 연결하고 Slack HTTPS 전송을 위해 egress에도 연결한다. 추가 persistent credential 설정은 별도 승인이 필요하다. 이 변경은 실제 credential을 읽거나 복사하지 않는다.
- Alertmanager의 dedup/silence 상태는 named volume에 보존하고 내부 보존은 120시간이다. process crash/호스트 손실에 대해 모든 알림의 영구 발송을 보장하지 않는다. 필요하면 상태 volume도 승인된 백업 정책에 포함한다.

## 활성화 전 결정

1. 기존 앱 webhook이 가리키는 workspace/채널과 담당자를 실제 설정 관리자에게 확인한다. 동일 수신처 재사용과 Alertmanager의 지속적 사용, 합성 TEST/RESOLVED 2건 전송을 승인받는다. 실제 URL을 채팅으로 요청하지 않는다.
2. 공식 `prom/alertmanager` 이미지의 지원 버전(이 구성은 0.28 계열 설정 기준)과 manifest digest/호스트 아키텍처를 확인한다. `ALERTMANAGER_IMAGE=prom/alertmanager@sha256:<검증된 64자리 digest>`로 고정한다. 이번 작업은 네트워크 제한으로 digest를 조회하지 않았다. 가짜 digest로 활성화하지 않는다.
3. 운영자가 release 밖의 전용 파일(예: `/etc/jbnu-sugang-helper/secrets/alertmanager-slack-webhook`)에 승인된 webhook을 안전하게 준비한다. 기존 앱 `.env` 전체를 mount하지 않는다. parent 디렉터리 접근을 제한하고, 파일은 root 소유·컨테이너 UID/GID 65534가 읽을 수 있는 최소 권한으로 설정한다(예: root:65534, 0640). Compose local secrets는 실제 host 파일의 권한을 따르므로 `uid/gid/mode`로 안전해진다고 가정하지 않는다. 권한 변경도 별도 승인 절차를 따른다.
4. 공개 target 목록인 `prometheus-targets.json`을 release 밖 `/etc/jbnu-sugang-helper/alertmanager-targets.json`에 준비한다. runtime `.env`에는 다음 **경로/옵션만** 보존한다. webhook 원문은 없다.

```dotenv
ALERTING_ENABLED=true
ALERTMANAGER_IMAGE=prom/alertmanager@sha256:<검증된 digest>
ALERTMANAGER_SLACK_WEBHOOK_FILE=/etc/jbnu-sugang-helper/secrets/alertmanager-slack-webhook
PROMETHEUS_ALERTMANAGER_TARGETS_FILE=/etc/jbnu-sugang-helper/alertmanager-targets.json
ALERTMANAGER_PORT_HOST=9093
```

`ALERTING_ENABLED=true`일 때 release deploy가 추가 Compose 파일을 계속 사용한다. target 경로도 runtime `.env`에서 유지하므로 다음 배포가 빈 기본 discovery로 덮어쓰지 않는다. secret/targets 파일은 release 동기화 대상 밖에 둔다. `ALERTING_ENABLED`는 deploy 스크립트 옵션이며 일반 `docker compose` 명령이 자동으로 읽는 프로필은 아니다. 수동 작업에서는 항상 base와 overlay를 함께 `-f`로 지정한다.

## 로컬·배포 검증 순서

```sh
python3 infra/scripts/test-alert-delivery.py
python3 infra/scripts/test-alerting-settings.py
# 승인·검증된 이미지가 로컬에 이미 존재할 때만. 네트워크/실제 secret 불필요.
ALERTMANAGER_IMAGE='prom/alertmanager@sha256:<검증된 digest>' \
  bash infra/scripts/validate-alerting-config.sh
```

Python 검사는 표준 라이브러리 mock/정적 계약이다. shell 검사는 `--pull never --network none`으로 실제 `amtool check-config`와 route 검사를 수행하며 `.invalid` fixture만 읽는다. mock 테스트를 실제 binary/전송 검증으로 표현하지 않는다. 현재 Docker socket 제한으로 binary 검사는 실행하지 못했다. CI에서도 사용 가능한 검증된 digest를 제공한 뒤 별도 실행해야 한다.

배포 전 `validate-alerting-config.sh --static-only`는 환경값 `ALERTMANAGER_IMAGE`, `ALERTMANAGER_SLACK_WEBHOOK_FILE`, `PROMETHEUS_ALERTMANAGER_TARGETS_FILE`, `ALERTING_RELEASE_ROOT`(실제 release 루트), 선택 `ALERTMANAGER_PORT_HOST`를 검증한다. 이미지 digest 형식, release 밖의 실제 절대경로/일반 파일, 정확한 내부 target JSON, 포트 범위를 확인한다. secret은 내용 대신 존재/파일 유형/위치만 확인한다. 이미지 공급망·secret 유효성·컨테이너 읽기 권한·실제 수신을 증명하지 않는다. release symlink 안으로 되돌아가는 파일도 거부한다.

승인된 배포 후 Alertmanager `/-/ready`, Prometheus `/api/v1/alertmanagers`의 active target, `/targets`의 alertmanager job, `/rules`에서 정상 적재/평가를 확인한다. receiver 이미지 digest와 실제 배포 SHA도 기록한다. `/api/v2/status`, `amtool config show`, `docker compose config` 전체 결과를 공유하지 않는다. 실제 설정·환경이 포함될 수 있다.

## TEST와 RESOLVED를 실제 수신 확인

CLI는 기본적으로 한 줄 JSON preview만 출력하며 아무 요청도 하지 않는다. 같은 test_id를 장애/복구에 사용하고 이전 테스트 ID는 재사용하지 않는다. 대상은 literal `http://127.0.0.1:<port>`만 허용하며 프록시/redirect/재시도는 없다. 외부 receiver URL이나 webhook을 이 도구에 입력하지 않는다.

```sh
python3 infra/scripts/test-alert-receiver.py --test-id operator-20261108-001
# 아래 두 명령은 수신처와 외부 전송 승인 이후에만 실행한다.
python3 infra/scripts/test-alert-receiver.py --test-id operator-20261108-001 --send --confirm-send
# Slack에서 같은 test_id의 [FIRING] TEST를 실제 확인한 뒤:
python3 infra/scripts/test-alert-receiver.py --test-id operator-20261108-001 --phase resolved --send --confirm-send
```

firing은 10분 뒤 자동 만료한다. group_wait 30초 이후에도 1~2분간 수신이 없으면 조사를 먼저 한다. 실패한 요청은 결과가 불명확할 수 있어 자동 재전송하지 않는다. API `accepted`는 Slack 수신 증거가 아니고 CLI는 항상 `receiptVerified:false`를 반환한다. Slack에서 같은 ID의 `[줍줍 문제 발생]` 점검 알림와 `[줍줍 경보 해제]` 점검 알림를 모두 확인하고 시각/담당자/수신 채널을 비밀 없는 운영 기록에 남긴다. Slack 전송 성공·실패는 `alertmanager_notifications_total{integration="slack"}`, `alertmanager_notifications_failed_total{integration="slack"}`로 함께 확인한다. 동일 경로가 막힌 상태에서는 그 경로로 자기 실패를 확실히 통지할 수 없다.

이 테스트는 **Alertmanager부터** 확인한다. Prometheus의 규칙 평가/전달까지 검증하려면 추가 승인 후 임시 합성 규칙을 운영 rule 디렉터리에 넣고 config 검증→Prometheus SIGHUP→rules/alerts 확인→실제 FIRING 수신을 진행한다. 기존 장애를 일부러 만들거나 서비스를 중단하지 않는다.

```yaml
groups:
  - name: approved-delivery-smoke
    rules:
      - alert: AlertDeliveryPrometheusSmokeTest
        expr: vector(1)
        labels:
          severity: info
          service: sugang-helper
          test_id: operator-unique-id
        annotations:
          summary: "TEST: Prometheus 규칙부터 수신까지 합성 점검"
```

FIRING 수신 후 같은 규칙의 `expr`를 `vector(0) == 1`로 바꾸어 다시 검증·reload하고 `[줍줍 경보 해제]` 수신을 확인한다. 그러면 규칙은 유지된 상태로 expression만 false가 되어 복구를 판정한다. 복구 수신 뒤 임시 파일을 제거·reload한다. 동일 이름/label을 유지하고 10분 이상 방치하지 않는다. Prometheus의 `prometheus_notifications_errors_total`과 알림 큐 상태도 확인한다. 이 임시 규칙은 저장소의 기본 rule 디렉터리에 넣지 않았다.

## 중지·실패 처리

잘못된 채널이면 테스트를 반복하지 말고 기존 test_id를 해결 처리한 뒤 승인된 절차로 target을 비우고 Alertmanager를 중지한다. runtime `.env`의 `ALERTING_ENABLED=false`만으로 이미 실행 중인 컨테이너가 삭제되지는 않는다. 먼저 승인된 base+overlay 명령으로 `stop alertmanager`, discovery를 `[]`로 전환하고 Prometheus 설정/target 상태를 재확인한다. 기존 secret과 데이터 volume은 자동 삭제하지 않는다.

같은 호스트의 Prometheus/Alertmanager/백업이 함께 내려가는 장애는 여기서 외부에 보낼 수 없다. 별도 장애 도메인의 uptime/dead-man 감시는 여전히 필요하다. 앱 전용 Slack 비동기 경로는 앱의 기존 전송 fixture 테스트와 별도의 승인된 실제 이벤트로 검증해야 한다.
