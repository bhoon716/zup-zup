# [P1][infra] 데이터·엣지·관측 네트워크 세분화

## 문제

현재 `sugang-helper-network-server`에 DB, Redis, 앱, Nginx Proxy Manager, Loki, Promtail이 함께 연결된다. 프록시가 데이터베이스에 접근할 수 있고, 관측 컴포넌트도 필요 이상으로 서버 네트워크에 노출된다.

## 완료 기준

- DB/Redis, 앱/프록시, management, observability 네트워크를 최소 권한으로 분리한다.
- NPM이 필요한 앱·Grafana 경로만 접근할 수 있다.
- Prometheus, Grafana, Loki, Promtail, Alertmanager 간 관측 경로가 유지된다.
- 내부 네트워크 멤버십과 포트 노출을 정책 테스트로 고정한다.
- Loki의 `auth_enabled: false`가 외부 네트워크에 노출되지 않는다.

## 근거

- `infra/docker-compose.yml`
- `infra/loki/loki-config.yaml`
- `infra/scripts/verify-compose-policy.sh`
