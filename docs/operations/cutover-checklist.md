# 신규 CI/CD cutover·rebuild 체크리스트

이 체크리스트는 ISSUE-127의 수동 검증 기록지다. 실제 hostname, SHA, volume 이름, 시각은 운영자가 별도 운영 로그에 채운다. 비밀번호·토큰·SSH 키는 적지 않는다.

## Cutover 전 보존

- [ ] 작업 시작 시각·operator·현재 앱 SHA를 기록했다.
- [ ] 기존 Compose 파일과 `.env`의 보관 위치/checksum을 기록했다.
- [ ] 기존 NPM proxy 설정, certificate 경로, DuckDNS hostname/reserved IP를 기록했다.
- [ ] 기존 NPM database·설정·인증서를 별도 백업하고 checksum을 기록했다. 새 edge 설정과 인증서가 검증되기 전까지 원본을 삭제하지 않는다.
- [ ] 기존 MySQL named volume 이름과 container 연결을 기록했다. `sugang-helper-db-data`를 삭제하거나 새 volume으로 바꾸지 않았다.
- [ ] OCI local DB backup script dry-run과 최신 dump checksum을 확인했다.
- [ ] `flyway_schema_history`와 현재 schema를 read-only로 확인했다.
- [ ] 기존 readiness·핵심 API·기본 데이터 조회 결과를 기준선으로 저장했다.

## 신규 stack 적용

- [ ] ARM64 GHCR image와 배포 대상 SHA를 확인했다.
- [ ] `/home/ubuntu/jbnu-sugang-helper`·`/home/ubuntu/jbnu-sugang-helper/.staging`·Ubuntu 운영 secret 권한을 확인했다. `.staging`은 배포 후 비어 있어야 한다.
- [ ] Compose가 기존 `sugang-helper-db-data` named volume을 재사용하는지 확인했다.
- [ ] Compose config, MySQL/Redis healthcheck, NPM upstream과 HTTPS readiness를 통과했다.
- [ ] Prometheus가 앱 `/actuator/prometheus`를 수집하고 Grafana datasource/dashboard가 보인다. Promtail은 실행하지 않는다.
- [ ] 기존 app을 중지한 뒤 Flyway baseline이 필요한지 확인하고, baseline history를 기록했다.
- [ ] Flyway `migrate`를 수행했다. migration 파일을 수정하지 않았다.
- [ ] 새 app readiness, HTTPS `/health`, CORS, 핵심 API, 기본 데이터 조회를 확인했다.

## 실패 복구

- [ ] acceptance 실패 시 새 app만 중지했다.
- [ ] 기존 Compose/NPM/SHA 자료를 삭제하지 않고 원래 stack을 복구했다.
- [ ] schema가 변경된 경우 기존 app과 호환되는지 확인했다. 호환되지 않으면 app을 억지로 시작하지 않았다.
- [ ] 복구 결과와 남은 위험을 운영 로그에 기록했다.

## Rehearsal

- [ ] traffic이 거의 없는 시간 또는 사전 합의된 점검 시간에 수행했다.
- [ ] 현재 정상 SHA → 이전 정상 SHA 수동 deploy workflow를 실행했다.
- [ ] readiness와 핵심 API smoke를 확인했다.
- [ ] 같은 수동 deploy workflow로 현재 정상 SHA를 다시 배포했다.
- [ ] 두 번의 결과와 downtime을 기록했다.
- [ ] 왕복 검증 전 기존 자료와 이전 이미지/volume을 삭제하지 않았다.

## 새 OCI 서버 재구축

- [ ] 새 ARM64 OCI A1과 reserved IP를 만들었다.
- [ ] 기존 `sugang-helper-db-data` named volume과 OCI local backup 경로를 확인했다.
- [ ] Docker/Compose, 기존 NPM edge, OCI `ubuntu` 사용자, firewall, SSH 연결을 bootstrap했다.
- [ ] Ubuntu root `.env`, 앱 전용 `apps/server/.env`, Firebase 파일을 별도 경로에서 입력했다.
- [ ] 승인된 `IMAGE_TAG`를 pull하고 readiness/HTTPS/smoke를 확인했다.
- [ ] DuckDNS가 reserved IP를 가리키는지 확인했다.
- [ ] acceptance와 rollback rehearsal이 끝날 때까지 기존 서버를 폐기하지 않았다.

## 명시적 한계

- named volume과 OCI local dump는 같은 host 장애를 함께 겪을 수 있다. host loss 보호가 필요하면 off-host/Object Storage 백업을 별도로 추가해야 한다.
- DB 자동 rollback, 인프라 `latest` rollback, 의도적인 깨진 production image 배포는 하지 않는다. 이전 SHA 재배포는 schema 호환성을 확인한 앱 재배포일 뿐이다.
