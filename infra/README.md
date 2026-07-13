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
docker compose up -d
```

## 검증 스크립트

- `./scripts/verify-compose-policy.sh`
- `./scripts/verify-log-policy.sh`
- `./scripts/backup-log-state.sh`
- `./scripts/restore-log-state.sh`
- `./scripts/test-redis-state-recovery.sh`
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

## 관측

- Prometheus는 `/actuator/prometheus`를 스크랩합니다.
- Grafana는 Prometheus와 Loki를 데이터소스로 사용합니다.
- Promtail은 `/var/log`를 읽어 Loki로 전달합니다.

## Related Docs

- [트러블슈팅 / 측정 기록](../docs/troubleshooting.md)
- [모노레포 전환 결정](../docs/decisions/2026-07-08-monorepo-migration-web-vercel-server-infra.md)

## Operational Notes

- 인프라는 운영과 복구를 먼저 생각합니다.
- 이 문서는 Docker Compose, 관측, 로그 정책을 빠르게 파악할 때 유용합니다.
