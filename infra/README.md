# 줍줍 Infra

<p align="center">
  서비스 운영을 떠받치는 인프라 계층. 데이터, 로그, 메트릭, 프록시를 관리한다.
</p>

줍줍 Infra는 서비스가 실제로 돌아가게 만드는 구성 요소를 모아둔 영역입니다.  
로컬과 운영 환경의 차이를 줄이고, 상태를 관찰하고, 복구 가능한 운영 구성을 유지하는 데 초점을 둡니다.

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 역할 | 데이터베이스, 캐시, 메트릭, 로그, 프록시 |
| 성격 | Docker Compose 기반 운영 스택 |
| 대상 | OCI CPU 인스턴스의 운영 환경 |
| 로그 | `/var/log/jbnu-sugang-helper/*` |

## 왜 이 인프라가 필요한가

- 서비스는 코드만으로 끝나지 않고, 로그와 메트릭이 있어야 운영할 수 있습니다.
- 개발 환경과 운영 환경이 너무 다르면 배포할 때마다 예외가 생깁니다.
- 작은 서비스라도 관측과 복구 절차가 없으면 유지보수가 어렵습니다.

## 주요 구성

| 구성 요소 | 역할 |
| --- | --- |
| MySQL | 주요 영속 데이터 저장 |
| Redis | 캐시와 상태성 데이터 |
| Prometheus | 메트릭 수집 |
| Grafana | 대시보드와 시각화 |
| Loki | 로그 집계 |
| Promtail | 로그 수집 에이전트 |
| Nginx Proxy Manager | 프록시와 TLS/도메인 운영 |

## 설계 포인트

| 선택 | 이유 | 효과 |
| --- | --- | --- |
| Docker Compose | 전체 스택을 한 번에 재현하기 위해 | 실행과 복구가 단순해집니다 |
| MySQL + Redis | 서비스 성격에 맞는 최소 운영 구성을 유지하기 위해 | 비용과 복잡도를 낮출 수 있습니다 |
| Prometheus + Grafana + Loki + Promtail | 지표와 로그를 함께 보기 위해 | 문제 원인 추적이 쉬워집니다 |
| Nginx Proxy Manager | 도메인과 프록시 설정을 손쉽게 관리하기 위해 | 운영 변경이 빠릅니다 |

## 실행 방법

```bash
cd infra
cp .env.example .env
# .env의 비밀값·호스트 경로를 환경에 맞게 수정한다.
bash scripts/verify-compose-policy.sh docker-compose.yml
docker compose --env-file .env up -d
```

`docker-compose.yml`의 필수 보간 변수는 값이 없으면 시작 전에 실패합니다. `.env`를 만들지 않거나 필수값을 비워 둔 상태에서 `docker compose up -d`를 실행하지 않습니다. `.env`에는 비밀값이 포함될 수 있으므로 저장소에 커밋하지 않습니다.

`DOCKER_NETWORK_MTU`는 로컬 기본값 `1500`을 사용합니다. OCI 호스트에서 jumbo frame을 실제로 확인한 경우에만 운영용 `infra/.env`에 `DOCKER_NETWORK_MTU=9000`을 설정합니다. Compose 정책 검사는 `1500`과 `9000` 이외의 값을 거부합니다.

네트워크는 데이터(`db`, `redis`, `app`, `migrate`), 엣지(`app`, `grafana`, `nginx-proxy-manager`), 관리(`app`, `prometheus`), 관측(`alertmanager`, `grafana`, `loki`, `prometheus`, `promtail`)으로 분리됩니다. 데이터·관리·관측망은 내부 전용이고, 엣지망만 프록시의 외부 인증서 갱신 egress를 허용합니다.

Promtail은 민감한 호스트 로그 전체를 수집하지 않고 `/var/log/jbnu-sugang-helper/*/*.log` 애플리케이션 로그 allowlist만 읽습니다.

## 검증 스크립트

- `./scripts/verify-compose-policy.sh`

### Observability durability and alerts

Prometheus stores its TSDB under `/var/lib/jbnu-sugang-helper/prometheus` with a 30-day/20GB retention cap. Alertmanager stores state under `/var/lib/jbnu-sugang-helper/alertmanager` and receives Prometheus SLO, DLQ, provider-circuit, and crawler-freshness alerts. Set `ALERTMANAGER_WEBHOOK_URL` to the reviewed operator/Discord webhook before deployment; the example value is intentionally a placeholder. The Grafana notification dashboard is provisioned from `grafana/dashboards/notification-slo-dashboard.json`.

### Resource budget

The single-host budget is explicit: `db` 2 CPU/2 GiB/256 pids, `app` 2 CPU/1.5 GiB/512 pids, `redis` 0.5 CPU/512 MiB/128 pids, Prometheus 1 CPU/1 GiB/256 pids, Alertmanager 0.25 CPU/256 MiB/128 pids, Grafana 0.75 CPU/768 MiB/256 pids, Loki 1 CPU/1 GiB/512 pids, Promtail 0.25 CPU/256 MiB/128 pids, and Nginx Proxy Manager 0.5 CPU/512 MiB/256 pids. The app uses graceful Spring shutdown with a 30-second phase and the notification worker remains bounded at 8 threads plus a 32-item queue. `verify-compose-policy.sh` rejects missing or changed budgets before deployment.
- `./scripts/verify-log-policy.sh`
- `./scripts/backup-log-state.sh`
- `./scripts/restore-log-state.sh`
- `./scripts/test-redis-state-recovery.sh`
- `./scripts/test-db-service-accounts.sh`
- `./scripts/backup-dr-state.sh`
- `./scripts/restore-dr-state.sh`
- `./scripts/test-dr-state-recovery.sh`
- `./scripts/test-deploy-app.sh`

## Redis 준비와 복구

Redis는 `/var/lib/jbnu-sugang-helper/redis` host bind에 AOF를 저장합니다. 처음 배포하기 전에 root로 mount directory를 준비합니다.

```bash
sudo bash scripts/prepare-app-host-directories.sh
```

이 directory는 Redis UID/GID `999:1000`의 owner-only mode `0700`으로 만들어져 session·refresh state를 일반 호스트 계정에서 읽지 못하게 합니다.

Redis는 인증된 `PING`이 성공해야 healthy가 되고, 앱 Docker healthcheck는 DB와 Redis를 포함한 `/actuator/health/readiness`를 확인합니다. 앱 배포도 Redis health preflight를 먼저 통과해야 합니다.

정상 container restart는 AOF state를 보존합니다. 갑작스러운 host/storage 장애에서는 `appendfsync everysec` 정책상 마지막 약 1초의 쓰기가 유실될 수 있습니다. refresh record가 없으면 인증은 fail-closed입니다.

백업은 일관된 AOF archive를 만들기 위해 Redis를 잠시 멈춥니다. 트래픽이 낮은 시간에 실행합니다.

```bash
sudo bash scripts/backup-redis-state.sh
sudo CONFIRM_REDIS_RESTORE=RESTORE \
  bash scripts/restore-redis-state.sh /var/backups/jbnu-sugang-helper/redis-state/redis-state-<timestamp>.tar.gz
```

restore는 checksum과 archive path를 검증하고, 시작 실패 시 이전 state로 되돌립니다. 성공 뒤 이전 state directory는 root가 검증·정리할 때까지 남깁니다. 배포된 전체 stack의 Redis outage/restart drill은 다음으로 실행합니다.

```bash
bash scripts/redis-restart-smoke.sh
```

Redis preflight 실패는 현재 deployment Discord failure channel에 전달됩니다. 지속적인 Prometheus/Alertmanager notification route는 ISSUE-099에서 추가합니다.

## DB 권한과 재해 복구

애플리케이션은 DML 전용 DB 계정으로만 실행합니다. Flyway DDL은 배포 직전 일회성 `migrate` container가 별도 계정으로 수행하므로, runtime app 환경에는 MySQL root·migrator·backup credential이 들어가지 않습니다. 기존 volume에도 배포 preflight가 idempotent하게 계정을 생성·정렬합니다.

DB와 업로드 파일은 같은 복구 시점을 보장하기 위해 백업 중 app, Grafana, NPM을 잠시 멈춥니다. archive는 MySQL dump·binlog·uploads·Grafana·NPM data/cert를 암호화해 묶고 checksum과 14일 보존 정책을 적용합니다.

```bash
sudo BACKUP_ENCRYPTION_PASSWORD_FILE=/etc/jbnu-sugang-helper/backup-passphrase \
  BACKUP_AUTHENTICATION_KEY_FILE=/etc/jbnu-sugang-helper/backup-authentication-key \
  bash scripts/backup-dr-state.sh

sudo CONFIRM_DR_RESTORE=RESTORE \
  BACKUP_ENCRYPTION_PASSWORD_FILE=/etc/jbnu-sugang-helper/backup-passphrase \
  BACKUP_AUTHENTICATION_KEY_FILE=/etc/jbnu-sugang-helper/backup-authentication-key \
  bash scripts/restore-dr-state.sh \
  /mnt/secondary-backup/dr-state-<timestamp>.tar.gz.enc
```

운영 backup archive·checksum·HMAC sidecar는 반드시 별도 disk/host에도 보관합니다. passphrase와 별도 HMAC key는 archive storage와 분리된 secret manager 또는 offline escrow에 보관합니다. clean-host 자동 복구 drill은 `scripts/test-dr-state-recovery.sh`이며, 전체 정책과 OAuth 확인 경계는 [disaster-recovery-policy.md](../docs/disaster-recovery-policy.md)를 따릅니다.

## 관측

- Prometheus는 `/actuator/prometheus`를 스크랩합니다.
- Grafana는 Prometheus와 Loki를 데이터소스로 사용합니다.
- Promtail은 `/var/log/jbnu-sugang-helper/*/*.log` allowlist만 읽어 Loki로 전달합니다.

## Related Docs

- [트러블슈팅 / 측정 기록](../docs/troubleshooting.md)
- [모노레포 전환 결정](../docs/decisions/2026-07-08-monorepo-migration-web-vercel-server-infra.md)

## Operational Notes

- 인프라는 운영과 복구를 먼저 생각합니다.
- 이 문서는 Docker Compose, 관측, 로그 정책을 빠르게 파악할 때 유용합니다.
