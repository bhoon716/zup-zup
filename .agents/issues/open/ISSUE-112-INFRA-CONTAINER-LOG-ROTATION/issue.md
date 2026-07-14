# [P1][infra] DB·Redis 컨테이너 로그 로테이션 보장

## 문제

Prometheus, Alertmanager, Grafana, Loki, Promtail, 앱에는 Docker `json-file` 로그 제한이 있지만 DB와 Redis에는 없다. 기본 Docker 로그 파일이 무제한으로 증가해 단일 OCI 호스트의 디스크를 소진할 수 있다.

## 완료 기준

- DB와 Redis에도 공통 `json-file` `max-size`·`max-file` 정책을 적용한다.
- 모든 장기 실행 서비스가 동일한 로그 로테이션 계약을 갖는다.
- 검증 스크립트가 서비스별 누락을 검출한다.
- 로테이션 후에도 장애 분석에 필요한 로그가 보존된다.

## 근거

- `infra/docker-compose.yml`
- `infra/scripts/verify-log-policy.sh`
