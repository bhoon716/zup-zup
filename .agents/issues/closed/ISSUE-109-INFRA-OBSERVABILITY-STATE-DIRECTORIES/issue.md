# [P1][infra] Loki·Promtail 영속 디렉터리 초기화와 권한 보장

## 문제

호스트 디렉터리 준비 스크립트가 Prometheus, Alertmanager, Grafana 디렉터리는 생성하지만 Loki와 Promtail 디렉터리는 생성하지 않는다. Docker가 bind source를 자동 생성하면 root 소유가 되어 Loki(UID 10001)가 WAL과 chunk를 쓰지 못할 수 있다.

## 완료 기준

- `prepare-app-host-directories.sh`가 Loki와 Promtail 영속 디렉터리를 생성한다.
- Loki 실행 UID/GID에 맞는 소유자와 최소 권한을 적용한다.
- 깨끗한 호스트에서 준비 스크립트 후 Compose 기동이 가능한지 검증한다.
- 정책 검증이 두 디렉터리의 존재·권한·마운트를 확인한다.

## 근거

- `infra/scripts/prepare-app-host-directories.sh`
- `infra/docker-compose.yml`
- `infra/scripts/backup-log-state.sh`
