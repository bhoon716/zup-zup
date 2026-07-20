# [P2][infra] 관측·프록시 서비스 readiness healthcheck 추가

## 문제

DB, Redis, 앱만 실질적인 healthcheck와 의존성 조건을 갖고 있으며 Grafana, Prometheus, Alertmanager, Loki, Promtail, NPM은 프로세스가 떠 있는지만으로 healthy처럼 취급될 수 있다.

## 완료 기준

- 각 서비스의 공식 readiness/health endpoint 또는 안전한 로컬 명령을 healthcheck로 정의한다.
- 실패 시 재시작·알림·배포 smoke가 실제 비정상 상태를 감지한다.
- healthcheck가 외부 포트나 인증 우회에 의존하지 않는다.
- ARM64 로컬 및 OCI 환경에서 오탐·과도한 재시작 없이 동작한다.

## 근거

- `infra/docker-compose.yml`
- `infra/scripts/test-observability-smoke.sh`
- `infra/prometheus/prometheus.yml`
