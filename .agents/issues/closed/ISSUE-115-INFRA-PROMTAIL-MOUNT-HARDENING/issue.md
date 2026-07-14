# [P2][infra] Promtail 호스트 로그 마운트 범위 축소

## 문제

Promtail이 `/var/log` 전체를 read-only로 마운트하고 `*log` 패턴으로 수집한다. 읽기 전용이지만 호스트의 다른 서비스 로그와 민감한 정보까지 관측 시스템으로 전송될 수 있다.

## 완료 기준

- 수집 대상 로그 경로를 서비스 운영에 필요한 allowlist로 제한한다.
- 애플리케이션 로그 수집과 positions 파일 저장은 유지한다.
- 시스템 로그가 필요하면 구체적인 경로·민감정보 마스킹 정책을 정의한다.
- Compose와 Promtail smoke가 허용된 로그만 수집하는지 검증한다.

## 근거

- `infra/docker-compose.yml`
- `infra/promtail/promtail-config.yaml`
- `infra/scripts/verify-log-policy.sh`
