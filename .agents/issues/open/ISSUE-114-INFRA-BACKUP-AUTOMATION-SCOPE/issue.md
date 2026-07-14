# [P1][infra] DR 백업 자동화와 관측 데이터 보존 범위 확정

## 문제

DR 백업과 로그 백업 스크립트는 존재하지만 저장소 내 자동 실행 일정은 확인되지 않는다. 또한 DR 백업에는 DB, 업로드, Grafana, NPM만 포함되고 Prometheus/Loki/Alertmanager/Promtail 상태는 제외된다. 로그 백업 스크립트는 실행 중 상태를 직접 복사하며 암호화·무결성 검증이 없다.

## 완료 기준

- 운영 호스트에서 DR 백업과 Redis 백업의 실행 주기·실패 알림·보존 정책을 명시하고 자동화한다.
- Prometheus/Loki/Alertmanager/Promtail 상태를 보존할지, 재생성 가능한 데이터로 폐기할지 명시한다.
- 보존 대상 백업은 암호화·checksum·HMAC 및 별도 저장소 정책을 따른다.
- clean-host 복구 drill과 최근 백업 freshness 검증을 정기 실행한다.
- 기존 `backup-log-state.sh`의 역할을 DR 정책과 일치시키거나 deprecated 처리한다.

## 근거

- `infra/scripts/backup-dr-state.sh`
- `infra/scripts/backup-log-state.sh`
- `infra/scripts/restore-dr-state.sh`
- `docs/disaster-recovery-policy.md`
- `infra/README.md`
