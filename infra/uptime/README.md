# 외부 readiness monitor

외부 uptime provider는 Loki·Grafana Alloy·Grafana 로그 검색 및 앱 metrics용 Prometheus와 별도로 공개 readiness를 감시한다. Alertmanager는 사용하지 않으며, provider는 저장소에 고정하지 않고 운영자가 선택한 provider의 비밀값도 저장소에 기록하지 않는다.

필수 설정:

- URL: `https://<API_HOST>/health/ready`
- interval: 60초
- 실패 알림: 연속 2회 실패 후 이메일
- 복구 알림: recovery 이메일 활성화
- 성공 조건: HTTP 200, response body에 내부 세부정보가 없음
- timeout: provider 기본값보다 짧은 운영 승인값(예: 10초 이하)

검증 절차:

1. `curl --fail --silent --show-error https://<API_HOST>/health/ready`가 성공하는지 확인한다.
2. 운영자만 읽을 수 있는 provider 화면에서 60초 주기와 2회 실패 조건을 확인한다.
3. maintenance 시 Nginx 또는 앱을 잠시 중지해 이메일이 정확히 두 번의 연속 실패 뒤 도착하는지 확인한다.
4. 서비스를 복구하고 recovery 이메일이 도착하는지 확인한다.

이 문서는 이메일 수신 주소나 provider API token을 저장하지 않는다. Slack 알림은 후속 운영 이슈로 남긴다.
