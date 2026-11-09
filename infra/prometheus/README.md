# 앱과 분리된 상태 점검 규칙

Prometheus가 `rules/alerts.yml`을 15초마다 평가한다. 앱이 멈춰도 Prometheus가 살아 있으면 앱 메트릭 수집 실패를 판정할 수 있다. **경보 판정과 외부 알림 전달은 별개다.** 승인 후 사용하는 [Alertmanager 추가 구성](../alertmanager/README.md)을 준비했지만 기본 target은 비어 있으며 실제 수신처·자격증명을 설정하지 않았다. 운영 반영 및 실제 알림 수신은 검증하지 않았다.

## 무엇을 보는가

- 앱, Loki, Alloy, Prometheus의 `/metrics` 수집 실패 또는 타깃 누락: 2분 지속 시 경보.
- Prometheus 규칙 평가 실패 및 설정 재적용 실패.
- Outbox/강의 데이터 점검: 마지막 성공이 3분을 넘거나 상태가 실패이거나 메트릭이 누락된 상태가 2분 지속. 앱 자체가 내려가면 중복 경보를 억제한다.
- 전송 가능한 알림: READY 대기가 있고 가장 오래된 대기가 5분을 넘은 상태가 다시 5분 지속. `queue_depth`는 처리 중 스레드 수가 아니라 DB에서 확인한 실제 전송 가능 건수다.
- 알림 대상 생성 대기: 10분 연속 1건 이상. 트래픽이 계속 있어도 대기 수가 0으로 돌아오지 않으면 경보가 날 수 있으므로 운영 관찰 후 임계값을 조정한다.
- 강의 데이터 오래됨: Prometheus의 현재 시각에서 마지막 성공 타임스탬프를 빼서 앱의 freshness 임계값과 직접 비교한다. 값이 멈춰도 시간이 흐르면 감지하며, 성공 기록 없음(0), 타임스탬프/임계값 누락, 잘못된 임계값도 5분 지속 시 경보한다. 앱이 내려가면 중복 경보를 억제한다. 앱의 샘플링된 `crawler_data_stale` 플래그에는 의존하지 않는다.

`notification_outbox_oldest_ready_timestamp_seconds`는 빈 대기열에서 0이다. 반드시 대기 수 > 0 조건과 함께 사용한다. 점검 타임스탬프는 60초 주기의 **성공한** 조회에서만 갱신되어야 한다. READY는 재시도 시각에 도달한 PENDING 또는 임대가 만료된 PROCESSING이다. 임계값은 초기 운영 기준이며 서비스 목표를 보장하는 수치가 아니다.

## 화면과 진단

Grafana 대시보드는 사용자가 Grafana 페이지에서 관리한다. 이번 변경은 대시보드 JSON/provisioning을 수정하거나 새 대시보드를 추가하지 않는다. UI에서 `up`, 활성 경보, 실제 대기 수, 점검 경과 시간을 함께 확인하는 것이 좋다. 기존 패널의 `or vector(0)`은 수집 누락을 정상 0처럼 보이게 할 수 있으므로 UI에서 검토해야 한다. `No data`는 정상 의미가 아니며, 활성 경보가 없을 때 ALERTS 그래프도 비어 있을 수 있다.

Prometheus `/targets`에서 기본 네 job과 `host-jobs`의 수집 상태를 확인하고 `/rules`, `/alerts`에서 규칙 적재·평가를 확인한다. 이 엔드포인트는 내부 네트워크 전용이며 외부 포트를 열지 않았다. 승인된 Alertmanager를 활성화하면 같은 discovery 파일로 receiver의 `/metrics`도 수집한다. 수집 성공은 컴포넌트의 모든 기능이나 로그·외부 알림 전달 성공을 증명하지 않는다.

Alloy는 배포 스크립트의 Compose 프로젝트 이름 `sugang-helper`만 수집한다. 프로젝트 이름을 변경하면 relabel 규칙도 함께 수정해야 한다. Docker socket의 `:ro` 마운트는 Docker API 쓰기 권한을 없애는 보안 경계가 아니므로 호스트 접근 권한은 여전히 중요하다.

## 검증

```sh
bash infra/scripts/test-observability-contract.sh
python3 infra/scripts/test-json-log-pipeline.py
python3 infra/scripts/test-alert-delivery.py
python3 infra/scripts/test-alerting-settings.py
```

첫 명령은 저장소에 고정된 이미지로 Alloy/Loki/Prometheus 설정과 promtool 경보 테스트를 검증한다. 테스트는 중단·누락·일시 장애·복구·점검 정지·적체·정상 상태를 포함한다. 두 번째는 격리된 로컬 Docker 환경에서 JSON 필드 검색, 스택 경계, 추적 검색, 다른 프로젝트 로그 제외를 검증한다. 마지막 두 개는 외부 전송·Docker·실제 secret 없이 CLI와 opt-in 설정을 검증한다. 실제 Alertmanager binary 검사와 TEST/RESOLVED 수신 절차는 receiver runbook을 따른다.

## 남은 운영 확인

- 승인된 외부 수신처와 전달 경로를 연결하고, 테스트 경보의 실제 수신·복구 알림까지 확인해야 한다. 현재 변경은 외부 메시지를 보내지 않는다.
- 이 Prometheus와 같은 호스트가 모두 멈추면 자체 규칙도 평가할 수 없다. 별도 장애 도메인의 외부 감시가 필요하다. 기존 외부 uptime 감시의 실제 실행·수신 상태도 별도 확인 대상이다.
- Loki/Alloy `up`은 종단간 로그 유실·로그 신선도·디스크 포화·백업 복구 성공을 보장하지 않는다. 조용한 서비스의 로그 부재를 장애로 오판하지 않도록 무조건적인 로그 없음 경보는 넣지 않았다.
- DB/Redis exporter, 호스트 exporter, Alertmanager는 새로 배포하지 않았다. [호스트 timer/textfile](../../docs/operations/host-job-automation.md)과 추가 경보 8개, Alertmanager opt-in 설정/테스트를 준비했지만 실제 실행·수신은 미검증이다. 앱의 기존 디스크 메트릭만으로 호스트 전체/관측 저장 볼륨의 상태를 판단하지 않는다.
