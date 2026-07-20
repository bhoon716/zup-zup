# 줍줍 Infra

이 디렉터리는 Vercel 프론트를 제외한 최소 운영 기반을 관리합니다. 현재 목표 런타임은 OCI A1 ARM64 한 대의 Docker Compose이며, 기본 서비스는 앱·MySQL·Redis 세 개입니다.

## 구성 원칙

| 구성 | 정책 |
| --- | --- |
| 앱 | `linux/arm64` commit SHA 이미지. 로컬에서는 Compose build, 운영에서는 GHCR pull |
| MySQL | Docker named volume이 OCI block-volume mount(`/var/lib/jbnu-sugang-helper/mysql`)을 가리킴 |
| Redis | 캐시·임시 데이터. persistence와 host port를 제공하지 않음 |
| Nginx | Compose 밖의 호스트 reverse proxy. 80→443 redirect, TLS termination, 민감 endpoint rate limit |
| certbot | 호스트 timer가 인증서를 갱신하고 `nginx -t` 성공 뒤 reload |
| 관측 | Loki + Grafana Alloy + Grafana를 `observability` profile로 운영. Prometheus/Alertmanager는 넣지 않음 |

상세 결정은 [단일 OCI CI/CD ADR](../docs/decisions/2026-07-19-single-oci-cicd.md), 실행 순서는 [운영 deployment runbook](../docs/operations/deployment.md)을 참고합니다.

## 로컬 실행

처음 한 번만 예시 환경을 복사하고 실제 로컬 비밀값·Firebase 파일 경로를 입력합니다. 예시 파일이나 실제 비밀값은 commit하지 않습니다.

```bash
cd infra
cp .env.example .env
cp ../apps/server/.env.example ../apps/server/.env
# infra/.env와 apps/server/.env의 placeholder를 로컬 값으로 채운다.

cd infra
docker compose up -d --build
```

`docker-compose.override.yml`은 Compose가 자동으로 읽습니다. 따라서 로컬 기본 실행에서 `-f`나 별도 profile을 지정할 필요가 없습니다. 기본 실행 서비스는 `app`, `db`, `redis`뿐이며 Nginx·관측 컨테이너는 시작되지 않습니다. 운영 배포는 `observability` profile을 함께 실행해 Loki, Alloy, Grafana를 유지합니다.
로컬 override는 `Dockerfile.local`에서 Gradle build까지 수행하므로 위 명령 하나로 앱 이미지가 생성됩니다. 운영/CI는 사전 생성한 `bootJar`를 사용하는 `Dockerfile`을 그대로 사용합니다.
기존 `infra` 프로젝트로 만든 `sugang-helper-app/mysql/redis` 컨테이너가 남아 있으면 첫 전환 때만 해당 컨테이너를 재생성해야 합니다. `docker rm -f sugang-helper-app sugang-helper-mysql sugang-helper-redis`는 컨테이너만 제거하며 named volume 데이터는 삭제하지 않습니다.

앱은 DB·Redis 전용 내부망과 외부 provider 호출·localhost 포트 공개를 위한 egress망에 함께 붙고, DB·Redis는 내부망에만 붙습니다.

DB migration은 앱 시작과 분리된 one-shot profile입니다.

```bash
# 새 로컬 DB의 첫 실행: migration을 적용한 뒤 history를 검증한다.
docker compose --profile migration run --rm migrate migrate
docker compose --profile migration run --rm migrate validate
```

## 로그 검색

운영 Compose는 `observability` profile에서 Loki, Grafana Alloy, Grafana를 실행합니다. Alloy는 Docker socket을 읽기 전용으로 사용해 Compose 컨테이너의 JSON 로그를 Loki로 전송하고, 호스트 Nginx 로그도 존재할 때 함께 수집합니다. Grafana는 `127.0.0.1:3000`에만 공개하므로 SSH 터널로 접속합니다.

```bash
docker compose --profile observability up -d loki alloy grafana
ssh -L 3000:127.0.0.1:3000 ubuntu@<api-host>
```

현재 Loki는 호스트 filesystem에 30일 retention으로 저장합니다. Object Storage 장기 보존과 복구 rehearsal은 bucket/IAM 계약을 확정한 뒤 별도 운영 이슈로 진행합니다.

이미 migration history가 있는 DB를 배포할 때는 운영 runbook대로 `validate` 성공 후 `migrate`를 실행합니다. `docker compose up` 자체는 DB schema를 자동 변경하지 않습니다.

로컬 Redis에는 volume이 없으므로 컨테이너를 재생성하면 데이터가 사라집니다. MySQL은 `sugang-helper-local-db-data` named volume을 사용합니다.

## Compose 계약 검증

실제 컨테이너를 띄우지 않고 ARM64/포트/healthcheck/권한/로그/volume 계약을 확인할 수 있습니다.

```bash
./scripts/test-runtime-contract.sh
./scripts/test-local-compose.sh
./scripts/verify-compose-policy.sh docker-compose.yml
./scripts/verify-log-policy.sh
```

기본 Compose config가 실패하면 비어 있는 secret을 임의로 넣지 말고 `.env`와 `apps/server/.env`의 누락 변수를 먼저 확인합니다.

## OCI bootstrap 요약

운영 호스트에서는 다음을 runbook 순서로 수행합니다.

1. ARM64 OCI A1, reserved public IP, DuckDNS `<API_HOST>`를 준비한다.
2. 80/443과 key-only SSH 22만 방화벽에서 열고 root/password SSH와 forwarding을 막는다.
3. OCI block volume을 `/var/lib/jbnu-sugang-helper/mysql`에 mount하고 다음을 root로 실행한다.

   ```bash
   sudo bash scripts/prepare-app-host-directories.sh
   ```

4. Docker/Compose, OCI 기본 `ubuntu` 사용자, root-only runtime secret, GHCR read-only token을 준비한다. 토큰은 임시로 안전한 파일에 저장하고, `GHCR_READ_USERNAME`과 `GHCR_READ_TOKEN_SOURCE`를 설치 명령에만 주입한다. 설치기는 토큰을 `${RELEASE_ROOT}/secrets/ghcr-read-token`에 root 소유 `0600`으로 복사하고 배포·rollback 때 `docker login ghcr.io --password-stdin`으로만 사용한다.

   ```bash
   sudo env \
     GHCR_READ_USERNAME=<github-username> \
     GHCR_READ_TOKEN_SOURCE=/path/to/ghcr-read-token \
     bash scripts/install-oci-wrappers.sh
   ```

   설치 후 `${RELEASE_ROOT}/.env.runtime`에 `GHCR_READ_USERNAME=<github-username>`을 기록한다. 토큰 값과 Actions 개인키는 OCI 문서나 저장소에 넣지 않는다.
5. 호스트 Nginx를 설치하고 site template을 실제 DuckDNS hostname으로 render한다.

   ```bash
   sudo API_HOST=<api-duckdns-fqdn> CERTBOT_EMAIL=<운영자-이메일> \
     bash scripts/bootstrap-nginx.sh
   ```

   이 스크립트는 rate-limit zone·proxy snippet을 먼저 설치하고, HTTP ACME 설정으로 인증서를 발급한 뒤 최종 TLS site를 적용합니다. `infra/nginx/conf.d/00-sugang-helper-rate-limit.conf`, `infra/nginx/snippets/jbnu-sugang-helper-proxy.conf`를 별도로 먼저 설치하지 않습니다.
6. certbot timer와 renew hook(`certbot renew → nginx -t → systemctl reload nginx`)이 설정되었는지 확인한다.
7. `bash scripts/verify-nginx-host-config.sh`와 `curl https://<API_HOST>/health/ready`로 확인한다.

세부적인 atomic file transfer, Flyway baseline/validate/migrate, rollback, cutover, 서버 교체 절차는 runbook을 그대로 따릅니다.

## 외부 uptime monitor

`https://<API_HOST>/health/ready`를 60초마다 확인하고 2회 연속 실패와 복구를 이메일로 알리도록 provider를 설정합니다. 응답에는 secret이나 DB 오류 세부정보를 넣지 않습니다. provider token·수신 주소는 저장소가 아닌 provider secret에 보관합니다. 검증 체크리스트는 [`uptime/README.md`](./uptime/README.md)에 있습니다.

호스트에서 실제 응답과 민감 정보 노출 여부를 확인하려면 다음을 실행합니다.

```bash
API_HOST=<api-duckdns-fqdn> bash scripts/test-uptime-contract.sh
```

## 파일과 비밀값

- `.env`, `secrets/`, host certificate/private key는 저장소에 커밋하지 않습니다.
- `DB_ROOT_PASSWORD`는 DB container에만, `DB_RUNTIME_PASSWORD`는 앱과 DB init에만, `DB_MIGRATOR_PASSWORD`는 one-shot migration에만 전달합니다.
- DB backup/restore는 이 최소 구성의 기능이 아닙니다. `DB_BACKUP_*` 변수를 새 runtime contract에 추가하지 않습니다.
- `infra/nginx-proxy-manager`와 Prometheus/Alertmanager 관련 기존 파일은 새 runtime에서 참조하지 않습니다. Loki/Alloy/Grafana 설정은 production Compose의 `observability` profile에서 참조합니다.

## 관련 문서

- [운영 배포·rollback·서버 교체 runbook](../docs/operations/deployment.md)
- [CI/CD 결정 ADR](../docs/decisions/2026-07-19-single-oci-cicd.md)
- [장애 복구 정책(기존 문서)](../docs/disaster-recovery-policy.md)
