# ADR: 단일 OCI 서버 최소 CI/CD 및 복구 구조

- 상태: Accepted (구현 진행 중)
- 결정일: 2026-07-19
- 범위: `apps/server`, `infra`, GitHub Actions, OCI 운영 호스트
- 관련 이슈: [ISSUE-123](../../.agents/issues/closed/ISSUE-123-PLATFORM-CICD-ADR-RUNBOOK-CONTRACT/issue.md), [ISSUE-124](../../.agents/issues/open/ISSUE-124-INFRA-OCI-RUNTIME-EDGE-FOUNDATION/issue.md), [ISSUE-125](../../.agents/issues/open/ISSUE-125-PLATFORM-PR-CI-ARM64-GHCR/issue.md), [ISSUE-126](../../.agents/issues/open/ISSUE-126-PLATFORM-MAIN-CD-DEPLOY-MIGRATION/issue.md), [ISSUE-127](../../.agents/issues/open/ISSUE-127-PLATFORM-ROLLBACK-CUTOVER-RECOVERY/issue.md), [ISSUE-128](../../.agents/issues/in-progress/ISSUE-128-PLATFORM-MINIMAL-CICD-DEPLOYMENT-CONTRACT/issue.md)
- 근거 로그: [ultra-grill-me 설계 로그](../../.agents/skills/ultra-grill-me/logs/2026-07-16-cicd-infra-redesign-grill.md)

## Context

현재 제품은 Vercel에 프론트를 배포하고, 백엔드·MySQL·Redis를 OCI A1 ARM64 서버 한 대에서 운영한다. 운영자는 한 명이고, 목표는 복잡한 플랫폼을 만드는 것이 아니라 다음을 재현 가능하게 만드는 것이다.

1. PR이 합쳐지기 전에 백엔드와 이미지가 검증된다.
2. `main` 병합 후 같은 commit을 OCI에 배포한다.
3. migration이나 readiness가 실패하면 더 진행하지 않는다.
4. 문제가 있는 앱은 이전 앱 이미지로 운영자가 되돌릴 수 있다.
5. 서버를 잃어도 같은 절차로 호스트와 앱을 다시 세울 수 있다.

고정된 RTO/RPO, 무중단 배포, DB 데이터 복구 보장은 현재 요구하지 않는다. 따라서 단일 호스트의 장애와 짧은 배포 중단은 명시적으로 수용한다.

## Decision

### 1. 배포 경계

- 프론트엔드는 기존 Vercel Git 배포를 유지한다.
- 백엔드는 GitHub Actions가 별도 CI/CD로 배포한다.
- `main`은 PR만 merge할 수 있고 필수 CI check를 통과해야 한다. 직접 push는 금지한다.
- `main`의 push가 production CD를 시작한다. PR merge를 기본 운영 흐름으로 사용하며 경로 필터로 배포를 생략하지 않는다.
- GitHub Actions concurrency는 사용하지 않는다. OCI deploy/rollback script가 동일한 server-side `flock`으로 중복 실행을 거부한다.

### 2. 이미지와 실행 환경

- 서버 앱 이미지는 private GHCR에 `linux/arm64` 단일 플랫폼으로 push한다.
- 앱 릴리스 식별자는 정확히 40자리 commit SHA다. `latest`를 앱의 배포 식별자로 사용하지 않는다.
- OCI는 root 소유 release env의 `IMAGE_TAG`를 사용한다. 새 SHA가 pull되고 migration·readiness가 성공한 뒤에만 `IMAGE_TAG`를 갱신한다.
- Docker Compose의 기본 서비스는 앱·MySQL·Redis로 두고, `observability` profile에 Loki·Grafana Alloy·Grafana를 둔다. MySQL·Redis는 현재 합의대로 Compose `latest`를 사용하되, 매 앱 배포의 preflight에서 pull/health를 확인한다. 이 선택은 공식 인프라 rollback을 제공하지 않는 위험 수용이다.
- Nginx는 컨테이너 이미지가 아니라 OCI 호스트 패키지 reverse proxy다. 인증서는 호스트 certbot timer와 renew hook으로 갱신하고, `nginx -t` 성공 후 reload한다.

### 3. CI와 CD

PR CI는 `pull_request`에서 MySQL·Redis service container를 시작하고 Flyway `validate`·`migrate`, 백엔드 테스트·빌드, ARM64 앱 이미지 빌드를 수행한다. main CD는 전체 테스트를 반복하지 않고 ARM64 이미지 build/push, OCI 배포, migration, readiness만 수행한다. E2E는 필수 merge gate가 아니며 별도 수동/주기 실행 대상으로 남긴다.

main CD의 순서는 다음과 같다.

```text
disk preflight
  -> MySQL/Redis Compose latest pull + health preflight
  -> 새 앱 SHA 이미지 build/push 또는 수동 SHA pull
  -> staging 파일 SCP + checksum/Compose 검증 + atomic move
  -> 기존 앱 중지
  -> 새 SHA one-off Flyway migrate
  -> 새 앱 시작
  -> readiness(앱+DB+Redis) 확인
  -> 성공한 경우에만 IMAGE_TAG 갱신
  -> 최근 앱 이미지 3개를 남기는 보수적 cleanup
```

preflight, 파일 검증, migration, readiness 중 하나라도 실패하면 다음 단계로 가지 않는다. migration 실패 시 DB를 자동 rollback하지 않고 앱도 자동 재기동하지 않는다. readiness 실패 시 새 컨테이너만 중지하고 기존 `IMAGE_TAG`를 유지한다. 운영자는 동일한 deploy workflow에 이전 SHA를 입력해 복구한다.

### 4. 원격 권한과 파일 적용

- Actions는 production Environment에 저장한 SSH 개인키만 사용한다.
- OCI의 GHCR read-only token과 runtime secret은 root 소유 파일에만 둔다. 배포·rollback wrapper가 token file을 `docker login ghcr.io --password-stdin`으로 소비하며, 실제 값은 저장소 문서에 기록하지 않는다.
- SSH는 전용 deploy 사용자, 고정 `known_hosts` fingerprint, `authorized_keys`의 command 제한·PTY/forwarding 차단을 사용한다.
- deploy 사용자는 staging 디렉터리에만 쓰고, `sudo`는 SHA·경로를 검증하는 고정 wrapper(`deploy`, `rollback`, `nginx-config-apply`)만 실행한다. `eval`과 임의 문자열 shell 실행은 금지한다. deploy/rollback wrapper는 동일한 host `flock`을 사용한다.
- Compose와 스크립트는 staging으로 전송한 뒤 checksum과 문법을 확인하고 atomic move한다. Nginx 설정은 앱 배포 경계 밖에서 별도로 관리한다.

### 5. 런타임과 네트워크

- MySQL 데이터는 OCI block volume을 backing으로 하는 Docker named volume에 둔다.
- Redis는 캐시·임시 데이터이며 persistence를 보장하지 않는다.
- MySQL·Redis healthcheck가 통과한 뒤 앱이 시작되고, 앱 readiness가 통과한 뒤 호스트 Nginx가 proxy한다.
- 앱은 DB·Redis 전용 `internal` runtime network와 외부 OAuth·메일 provider 호출 및 localhost publish를 위한 별도 egress network에 함께 붙인다. DB·Redis·migration 컨테이너는 runtime network에만 붙인다.
- 외부 공개 포트는 80/443과 key-only SSH 22뿐이다. 앱은 localhost에만 bind하고 DB·Redis host port는 열지 않는다.
- DuckDNS hostname과 OCI reserved public IP를 사용한다. HTTP는 ACME challenge를 제외하고 HTTPS로 redirect한다.
- CORS는 운영 Vercel origin과 필요한 localhost origin만 명시적으로 허용한다. 민감 endpoint에만 보수적인 Nginx IP별 rate limit을 둔다.
- 외부 uptime monitor는 민감 정보를 반환하지 않는 readiness endpoint를 1분마다 확인하고, 2회 연속 실패와 복구를 이메일로 알린다. Loki·Grafana Alloy·Grafana는 `observability` profile로 로그 검색을 제공하며, Prometheus·Alertmanager·Slack은 후속 범위다. Promtail은 사용하지 않는다.

### 6. Migration과 rollback

- migration은 versioned SQL과 Flyway로 관리한다.
- 이미 적용된 migration 파일 수정은 금지한다. checksum 불일치 시 중단하고, 운영 `clean`과 자동 `repair`는 금지한다.
- 기존 DB를 새 구조에 연결할 때는 schema·volume·Flyway history를 먼저 확인하고, 필요한 경우 baseline을 한 번 수동으로 만든 뒤 이후 migration만 자동 적용한다.
- 공식 rollback 범위는 앱 이미지뿐이다. Production CD `workflow_dispatch`에서 40자리 이전 SHA를 입력해 같은 deploy 경로를 재사용한다.
- 정상 배포 후 deployment-order history의 최근 앱 release/image 3개, 현재 실행 이미지, MySQL volume은 cleanup에서 보존한다. SHA의 사전식 순서로 보존 대상을 고르지 않는다.
- DB backup/restore는 이번 최소 설계의 완료 조건이 아니다. 현재 [disaster-recovery-policy](../disaster-recovery-policy.md)는 더 넓은 기존 정책을 설명하므로, 실제 백업을 도입할 때 별도 정합성 이슈를 만든다. block volume 자체는 백업이 아니다.

## Alternatives considered

| 선택 영역 | 채택안 | 검토한 대안 | 채택 이유와 포기한 것 |
| --- | --- | --- | --- |
| 배포 연결 | main push GitHub Actions → 제한된 SSH wrapper | 수동 SSH, self-hosted runner, workflow_run chain | 운영자가 매번 수동 실행하지 않아도 되고, PR CI와 main CD를 한 번씩만 실행한다. GitHub 장애 시 수동 break-glass 경로가 필요하다. |
| 이미지 저장소 | private GHCR | OCI Registry, 서버 직접 build, 이미지 SCP | 저장소와 CI가 이미 GitHub에 있고 ARM64 build를 재현하기 쉽다. GHCR token 관리가 필요하다. |
| 앱 식별자 | commit SHA tag | `latest`, digest만 사용 | 사람이 확인하기 쉽고 rollback 입력이 단순하다. tag가 재지정되지 않도록 push 권한을 제한한다. |
| 호스트 edge | 호스트 Nginx + certbot | Caddy, Nginx 컨테이너, load balancer | 이미 익숙하고 단일 서버에 충분하다. 인증서·패키지 업데이트를 runbook으로 관리해야 한다. |
| 런타임 | 한 OCI의 Compose | managed DB, 여러 VM, Kubernetes | 비용·운영 복잡도를 최소화한다. 서버 장애 시 전체 서비스가 중단된다. |
| migration 실패 | 앱 중지·one-shot migrate·DB 자동 rollback 없음 | Spring Boot 내장 Flyway, 자동 DB rollback | 앱 runtime 권한과 schema 권한을 분리하고 destructive 자동 복구를 피한다. 실패 시 수동 복구 시간이 필요하다. |
| rollback | 같은 deploy workflow의 수동 SHA 재배포 | 전용 rollback workflow, 자동 rollback, DB 포함 rollback | workflow 중복을 줄이고 기존 GHCR 이미지를 재사용한다. 운영자 대응이 필수다. |
| 인프라 이미지 | MySQL·Redis `latest` preflight | version tag, digest pinning | 사용자가 선택한 단순한 업데이트 경로를 따른다. 재현성과 인프라 rollback은 약하다. 문제가 반복되면 가장 먼저 version/digest로 되돌린다. |

## Tradeoffs and risks

- **단순성 우선:** 한 서버와 host Nginx로 구성 요소를 줄였지만 서버 장애 도메인이 하나다.
- **복구 범위 제한:** 앱 SHA는 되돌릴 수 있으나 DB 데이터와 MySQL·Redis `latest` 변경은 공식 rollback 대상이 아니다.
- **배포 중단 허용:** migration 동안 앱을 중지하므로 짧은 downtime이 발생한다. 무중단을 약속하지 않는다.
- **독립 배포 계약:** Vercel과 backend가 따로 배포되므로 breaking API 변경을 금지하고 add → switch → remove 순서를 지킨다.
- **관측 최소화:** 이메일과 30일 filesystem Loki 로그 검색으로 시작한다. Object Storage 장기 보존, Prometheus metric, Alertmanager 알림은 운영 요구가 확인되면 별도 계약으로 추가한다.

## Reversal conditions

다음 중 하나가 발생하면 이 ADR의 해당 결정을 재검토한다.

- MySQL·Redis `latest` 변경으로 호환성 회귀가 반복된다 → version tag 또는 digest pinning으로 전환한다.
- 단일 서버 downtime 또는 데이터 중요도가 허용 수준을 넘는다 → managed DB, 외부 backup, 두 번째 호스트/LB를 검토한다.
- 수동 rollback 시간이 장애 허용치를 넘는다 → 자동 health rollback을 제한된 조건에서 검토한다.
- GitHub Actions가 장시간 unavailable하다 → OCI에서 실행할 break-glass 절차를 별도 승인·문서화한다.

## Non-goals

- DB backup/restore 또는 point-in-time recovery 구축
- DB migration 자동 rollback
- MySQL·Redis `latest` 이미지의 공식 자동 rollback
- 무중단 배포, 고정 RTO/RPO, multi-node orchestration
- Terraform/IaC 전면 도입
- Prometheus/Alertmanager/Slack 알림과 Loki Object Storage 장기 보존
- Vercel 프론트 배포를 backend workflow에 결합

## Implementation map

- [ISSUE-124: OCI runtime·edge foundation](../../.agents/issues/open/ISSUE-124-INFRA-OCI-RUNTIME-EDGE-FOUNDATION/issue.md)
- [ISSUE-125: PR CI·ARM64·GHCR](../../.agents/issues/open/ISSUE-125-PLATFORM-PR-CI-ARM64-GHCR/issue.md)
- [ISSUE-126: main CD·migration·health](../../.agents/issues/open/ISSUE-126-PLATFORM-MAIN-CD-DEPLOY-MIGRATION/issue.md)
- [ISSUE-127: rollback·cutover·recovery](../../.agents/issues/open/ISSUE-127-PLATFORM-ROLLBACK-CUTOVER-RECOVERY/issue.md)
- [ISSUE-128: minimal CI/CD deployment contract](../../.agents/issues/in-progress/ISSUE-128-PLATFORM-MINIMAL-CICD-DEPLOYMENT-CONTRACT/issue.md)
- 실행 절차: [운영 배포 runbook](../operations/deployment.md)
