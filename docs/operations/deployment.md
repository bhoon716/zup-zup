# 운영 배포·rollback·서버 교체 runbook

> 상태: target contract. ISSUE-124~128 구현과 운영 검증이 끝나기 전에는 아래 명령을 그대로 실행하지 않는다. `<...>` 값은 운영자가 확인해 채우며, 비밀번호·토큰·SSH 키는 이 문서나 저장소에 넣지 않는다.

## 0. 변수와 안전 규칙

예시 변수는 실제 값이 아닌 placeholder다.

```text
SERVER_HOST=<reserved-ip-or-duckdns-hostname>
SERVER_USER=ubuntu
API_HOST=<api-duckdns-fqdn>
RELEASE_ROOT=/opt/jbnu-sugang-helper
STAGING_ROOT=/opt/jbnu-sugang-helper-staging
APP_IMAGE=ghcr.io/<owner>/<repository>/server
IMAGE_TAG=<exactly-40-hex-commit-sha>
PREVIOUS_IMAGE_TAG=<previous-40-hex-commit-sha>
MIN_FREE_PERCENT=<operator-approved-threshold>
```

- SHA는 정확히 `[0-9a-f]{40}`만 허용한다. 임의 shell 문자열, `latest` 앱 tag, 임의 경로를 입력하지 않는다.
- 운영 secret은 `<RELEASE_ROOT>/.env.runtime` 같은 root 소유 파일에 두고 `chmod 600`, 소유자 `root:root`로 설정한다. 값은 `DB_*`, `GHCR_READ_USERNAME`, OAuth/메일 등 실제 이름과 일치하는 배포 템플릿에서만 채운다. GHCR token 값은 `${RELEASE_ROOT}/secrets/ghcr-read-token`에만 `root:root`, `0600`으로 저장한다.
- GitHub 저장소 Actions secrets의 `SERVER_DOTENV`는 배포 시 `${RELEASE_ROOT}/.env.app`에 `root:root`, `0600` 권한으로 설치한다. 값은 이미지나 저장소에 기록하지 않는다.
- GitHub 저장소 Actions secrets에는 서버 주소·사용자·SSH private key·애플리케이션 dotenv만 저장한다. GHCR token과 인프라 runtime secret은 OCI root-only file에 둔다.
- 모든 실패 단계는 로그의 SHA·actor·시각·stage·result만 남긴다. secret, cookie, authorization header, 전체 환경 변수는 기록하지 않는다.

## 1. 최초 OCI bootstrap

### 1.1 호스트와 네트워크

1. OCI A1 ARM64 인스턴스를 만들고 reserved public IP를 연결한다.
2. `<API_HOST>`의 DuckDNS 레코드가 reserved IP를 가리키는지 확인한다. 실제 hostname은 이 문서에 하드코딩하지 않는다.
3. 운영 OS의 보안 업데이트를 적용하고 자동 보안 업데이트를 켜되 자동 재부팅은 켜지 않는다. Docker 업데이트는 이 runbook의 수동 절차로만 수행한다.
4. 방화벽/security list에서 80/443과 key-only SSH 22만 허용한다. 앱·MySQL·Redis 포트는 public/private host port로 노출하지 않는다.
5. OCI 기본 `ubuntu` 사용자를 사용하고 password login, root SSH login, 불필요한 forwarding/PTY를 막는다. Actions public key에는 `authorized_keys` command 제한을 적용한다.
6. workflow는 실행 시 `SERVER_HOST`의 SSH host key를 `ssh-keyscan`으로 수집한다. 고정 fingerprint를 사용하지 않는 단순 구성임을 인지한다.

### 1.2 Docker와 저장소

1. ARM64 Docker Engine과 Compose plugin을 설치하고 `docker version`/`docker compose version`으로 확인한다.
2. OCI block volume을 연결하고 Docker named volume의 backing path로 사용한다. 기존 DB를 이어 붙일 때 volume 이름과 filesystem을 먼저 확인한다.
3. 고정 디렉터리를 만들고 권한을 제한한다.

```bash
sudo install -d -o root -g root -m 0750 "$RELEASE_ROOT"
sudo install -d -o deploy -g deploy -m 0750 "$STAGING_ROOT"
sudo install -d -o root -g root -m 0700 "$RELEASE_ROOT/secrets"
```

4. root 소유 runtime env를 생성한다. 실제 secret은 password manager 또는 별도 승인된 절차로 입력한다.

```bash
sudo install -o root -g root -m 0600 /dev/null "$RELEASE_ROOT/.env.runtime"
# 편집은 운영자가 승인된 비밀 저장소에서 값을 복사해 직접 수행한다.
```

5. GHCR read-only token이 지정 package만 읽는지 확인하고, 설치기에서 `GHCR_READ_TOKEN_SOURCE`로 복사한 뒤 원본 임시 파일을 즉시 삭제한다. 배포·rollback은 root-only token file을 `docker login ghcr.io --password-stdin`으로 소비한다.
6. 저장소의 `infra/scripts/install-oci-wrappers.sh`를 검토한 뒤 GHCR read-only token 환경을 주입해 root 실행하고, `infra/security/jbnu-deploy.sudoers.example`를 `visudo -c`로 검증해 `ubuntu` 사용자가 고정 wrapper만 sudo할 수 있게 한다. deploy wrapper는 staging을 root 소유로 잠근 뒤 checksum을 검증한다.

### 1.3 호스트 Nginx와 certbot

1. 호스트 패키지로 Nginx와 certbot을 설치한다. Nginx는 Compose 서비스나 GHCR 이미지가 아니다.
2. `API_HOST=<fqdn> CERTBOT_EMAIL=<operator-email> sudo -E bash infra/scripts/bootstrap-nginx.sh`를 실행한다. 스크립트는 rate-limit zone·proxy snippet을 설치하고 HTTP-only ACME 설정을 먼저 검증한다.
3. HTTP webroot로 `<API_HOST>` 인증서를 발급한 뒤 최종 TLS site를 설치하고 `nginx -t`와 reload를 수행한다.
4. timer/deploy hook을 다음 순서로 구성한다.

```text
certbot renew
  -> nginx -t
  -> systemctl reload nginx
```

갱신 실패나 `nginx -t` 실패 시 기존 인증서와 Nginx 프로세스를 유지하고 이메일 알림을 확인한다.

### 1.4 서비스 시작 전 계약 확인

- MySQL·Redis Compose healthcheck가 각각 healthy가 되는지 확인한다.
- 운영 배포는 `observability` profile의 Loki·Alloy·Grafana도 healthy가 되는지 확인한다. Grafana는 호스트 `127.0.0.1:3000`에만 공개한다.
- 앱은 `127.0.0.1:<APP_PORT>`에만 bind하는지 확인한다.
- 앱만 runtime 내부망과 egress망에 모두 연결되고, DB·Redis·migration 컨테이너는 runtime 내부망에만 연결되는지 확인한다. egress망은 OAuth·메일 provider 통신과 호스트 Nginx의 localhost proxy에 필요하다.
- readiness endpoint는 앱·DB·Redis가 모두 준비된 경우에만 200을 반환하고, secret·DB error·내부 host 정보를 응답하지 않는다.
- CORS allowlist에 운영 Vercel origin과 필요한 localhost origin만 넣는다.

## 2. 상태 계약

| 상태 | 저장 위치/판정 | 변경 규칙 |
| --- | --- | --- |
| `IMAGE_TAG` | `$RELEASE_ROOT/.env.release`의 root 소유 `IMAGE_TAG=<40-hex>` | 새 이미지 pull·migration·readiness가 모두 성공한 뒤 한 번만 atomic 갱신 |
| readiness | `https://<API_HOST>/<readiness-path>` | 앱·DB·Redis가 모두 준비된 경우만 HTTP 200; 외부 monitor가 1분 주기로 확인 |
| migration | 새 앱 SHA의 one-shot Flyway 컨테이너 | `migrate` 한 번 실행; 실패 시 DB 자동 rollback·앱 자동 재기동 금지 |
| 앱 rollback | GitHub Actions `workflow_dispatch`에 이전 40자리 SHA 입력 | 새 컨테이너만 중지하고 기존 `IMAGE_TAG`는 성공 전까지 유지 |
| 이미지 보존 | GHCR와 OCI 로컬 이미지 | 최근 앱 3개, 현재 실행 이미지, MySQL volume을 보존. 성공한 배포 후 미사용 앱 이미지/캐시만 정리 |
| 인프라 이미지 | MySQL·Redis Compose `latest` | 앱 배포 전 pull/health preflight. 공식 infra rollback은 보장하지 않음 |
| Nginx | 호스트 OS package + `/etc/nginx` | 패키지/설정 변경은 별도 검증. certbot renew 성공 시에만 reload |

## 3. 정상 배포 (main CD)

GitHub Actions가 `main` push 후 실행하며, 저장소 Actions secrets를 사용한다. GitHub concurrency는 사용하지 않고 OCI deploy wrapper의 server-side `flock`으로 중복 실행을 거부한다. 운영자는 실패 시 다음 단계로 억지로 진행하지 않는다.

1. **입력 검증:** 일반 배포에서는 main push SHA를, 수동 배포에서는 입력한 정확한 40자리 SHA를 사용한다.
2. **디스크 preflight:** Docker root와 `$RELEASE_ROOT`가 설정한 최소 여유 공간(`MIN_FREE_PERCENT`)을 넘는지 확인한다. 부족하면 기존 앱을 중지하지 않고 종료한다. 자동 prune 후 계속하지 않는다.
3. **인프라 preflight:** MySQL·Redis `latest`와 Loki·Alloy·Grafana image를 pull하고 Compose healthcheck를 확인한다. 실패하면 앱 SHA pull 이후 단계로 가지 않는다. 호스트 Nginx는 앱 CD와 분리된 host 운영 절차로 관리한다.
4. **앱 이미지 pull:** GHCR read-only token으로 `APP_IMAGE:IMAGE_TAG`를 pull하고 ARM64 manifest와 이미지 존재를 확인한다. pull 실패 시 현재 서비스는 그대로 둔다.
5. **파일 staging:** Compose, migration, DB init, Loki·Alloy·Grafana 설정, `SERVER_DOTENV`, deploy wrapper support file을 `$STAGING_ROOT/<IMAGE_TAG>`에 SCP한다. root wrapper가 staging tree를 잠근 뒤 checksum·파일 owner/mode·`docker compose config`를 확인하고 필요한 파일만 atomic move한다. Nginx 설정은 staging하지 않는다.
6. **기존 앱 중지:** 검증된 새 이미지와 파일이 준비된 뒤에만 현재 앱 컨테이너를 중지한다. MySQL·Redis volume은 삭제하거나 recreate하지 않는다.
7. **Flyway migrate:** 새 앱 SHA의 one-shot migration 컨테이너를 실행해 versioned SQL을 적용한다. checksum/history/순서 오류가 나면 즉시 종료하고 자동 rollback을 시도하지 않는다.
8. **앱 시작:** dependency healthcheck 통과 후 새 앱 컨테이너를 시작한다.
9. **readiness gate:** 내부 healthcheck와 `<API_HOST>` readiness를 모두 확인한다. 200이 아니거나 timeout이면 새 앱 컨테이너만 중지하고 `IMAGE_TAG`를 바꾸지 않는다.
10. **상태 갱신:** readiness 성공 뒤 `.env.release`를 임시 파일에 작성하고 checksum 확인 후 atomic move한다. 기록에는 이전/새 SHA를 남긴다.
11. **cleanup:** `.release-history`에 성공 배포·재배포 순서를 기록하고 현재 실행 release를 포함한 최근 3개 release/image와 named volume은 남긴다. 오래된 release 디렉터리와 미사용 앱 이미지만 보수적으로 정리하며 `docker system prune -af`는 실행하지 않는다.
12. **알림:** 성공/실패 모두 SHA, actor, stage, elapsed time, result를 CI와 제한된 OCI deploy log에 남기고 이메일을 보낸다.

## 4. 실패 분기와 수동 rollback

### 4.1 preflight 또는 image pull 실패

- 기존 앱을 중지하지 않는다.
- 실패 원인(디스크, GHCR 인증, ARM64 manifest, MySQL/Redis/Loki/Alloy/Grafana health)을 확인하고 수정한다. Nginx 문법은 별도 host 절차에서 확인한다.
- 같은 SHA로 재시도한다. secret을 로그에 출력하거나 `latest` 앱 tag로 우회하지 않는다.
- 실패한 배포의 root-owned release 임시 디렉터리와 staging은 wrapper가 정리하므로 같은 SHA staging을 다시 전송할 수 있다. 상태 파일이 성공적으로 갱신된 뒤의 release는 보존한다.

### 4.2 Flyway migrate 실패

1. 앱은 중지된 상태로 둔다. 자동 재기동과 DB rollback을 시도하지 않는다.
2. Flyway 로그와 `flyway_schema_history`를 read-only로 확인한다. 이미 커밋된 SQL을 편집하지 않는다.
3. checksum mismatch는 파일을 원복하거나 승인된 수동 `repair` 절차를 검토한다. 운영 `clean`은 금지한다.
4. SQL 오류는 forward-fix migration을 새 버전으로 만들어 CI의 깨끗한 MySQL에서 먼저 검증한다.
5. schema가 호환되고 이전 앱으로 서비스해야 하는 경우 Production CD workflow에 이전 SHA를 입력해 재배포한다. 호환되지 않는 schema면 앱을 다시 시작하지 말고 수동 복구 결정을 기록한다.
6. 해결 후 `migrate` → readiness 순서로 재시도하고, 결과를 deploy log에 남긴다.

### 4.3 readiness 또는 핵심 smoke test 실패

1. 새 앱 컨테이너만 중지한다.
2. `$RELEASE_ROOT/.env.release`의 `IMAGE_TAG`가 이전 성공 SHA인지 확인한다. readiness 성공 전에는 값을 변경하지 않아야 한다.
3. Production CD workflow `workflow_dispatch`에서 `<PREVIOUS_IMAGE_TAG>`를 입력해 같은 deploy 경로로 재배포한다.
4. rollback 후 readiness와 핵심 API smoke test를 실행한다. 실패하면 계속 재배포하지 말고 운영자에게 이메일로 escalation한다.

rollback은 앱 이미지에만 적용된다. MySQL·Redis `latest`를 이전 버전으로 되돌리는 공식 절차는 없으므로, 해당 장애는 영향 범위를 확인한 뒤 호환되는 version tag/digest 전환을 별도 변경으로 승인한다.

## 5. 수동 이전 SHA 재배포 계약

- 공식 workflow: [`.github/workflows/cd.yml`](../../.github/workflows/cd.yml)
- trigger: Production CD `workflow_dispatch`
- input: `IMAGE_TAG=<exactly-40-hex-previous-commit-sha>`
- 권한: 저장소 Actions secrets, SSH runtime host-key setup, deploy wrapper, OCI server-side `flock`
- 순서: SHA 검증 → GHCR pull/ARM64 확인 → 필요한 파일 staging 검증 → 앱 중지 → one-shot migration → 앱 시작 → readiness → 성공 시에만 `IMAGE_TAG` 갱신
- 금지: `latest` 입력, DB volume 삭제, Flyway `clean`, 자동 DB rollback, 검증 전 상태 파일 갱신

## 6. Nginx 설정·인증서 운영

1. 운영자가 새 설정을 `$STAGING_ROOT/nginx/<run-id>`로 전송한다.
2. Nginx 전용 host 절차가 허용된 site 파일만 `/etc/nginx/sites-available`로 atomic move하고 symlink를 갱신한다.
3. `nginx -t`와 `systemctl reload nginx`가 모두 성공해야 적용 완료다. 실패하면 기존 설정을 유지한다.
4. certbot timer가 `renew`를 실행하고 deploy hook에서 `nginx -t` 후 reload한다.
5. HTTP는 ACME challenge를 제외하고 HTTPS로 redirect하고, `/health` 등 민감 endpoint에는 response body에 내부 정보를 넣지 않는다.

## 7. 기존 서버에서 새 구조로 cutover

초기 cutover는 새 서버를 장기간 병행하지 않고 같은 OCI에서 수행한다.

### 사전 확인

- 기존 Compose 파일, Nginx 설정, 현재 앱 image SHA, 환경 파일의 위치와 checksum을 별도로 기록한다.
- 기존 MySQL volume 이름·mount·schema·Flyway history를 read-only로 확인한다. volume을 새로 만들지 않는다.
- 기존 앱의 핵심 API smoke test와 readiness를 기준선으로 기록한다.
- 새 구조의 ARM64 image pull, Compose config, Nginx config, Flyway baseline plan을 사전에 검증한다.

### 전환

1. traffic이 낮은 시점에 운영자에게 작업 시작을 알린다. 고정 maintenance window를 보장하지는 않는다.
2. 기존 stack을 중지하되 기존 파일·volume·앱 SHA를 삭제하지 않는다.
3. `$RELEASE_ROOT`와 host Nginx 설정을 staging 검증 후 atomic move한다.
4. 기존 DB를 보존한 상태로 필요한 Flyway baseline을 한 번 적용하고 history를 확인한다.
5. 새 SHA migration과 앱 readiness를 실행한다.
6. `<API_HOST>`를 통한 HTTPS, CORS, 핵심 API, 기본 데이터 조회를 확인한다.

### 실패 시 복구

- acceptance가 하나라도 실패하면 새 앱을 중지하고 보존한 기존 Compose·Nginx 설정·앱 SHA로 되돌린다.
- DB schema가 변경된 뒤에는 기존 앱과의 호환성을 먼저 확인한다. 호환되지 않으면 기존 앱을 무리하게 시작하지 말고 forward-fix/수동 복구 결정을 기록한다.
- cutover 성공 후에도 첫 rollback rehearsal 전까지 기존 자료를 삭제하지 않는다.

## 8. 새 서버로 교체해야 할 때

1. 새 ARM64 OCI 인스턴스를 bootstrap하고 reserved IP와 block volume을 연결한다.
2. root secrets를 안전한 별도 경로에서 재입력하고 GHCR pull, Nginx/certbot, firewall, SSH fingerprint를 검증한다.
3. GHCR의 승인된 `IMAGE_TAG`를 pull해 Compose와 readiness를 확인한다.
4. 기존 volume을 연결할 수 있을 때만 DB를 이어 붙인다. block volume 자체는 backup이 아니며, volume을 잃으면 이 최소 설계에서는 DB 데이터를 복구할 수 없다.
5. smoke test 후 reserved IP/DuckDNS cutover를 수행한다.
6. 기존 서버는 acceptance와 rollback rehearsal이 끝날 때까지 폐기하지 않는다.

## 9. 로그·정리·정기 점검

- Docker json log는 `max-size`·`max-file`로 제한하고, Nginx 로그는 host logrotate로 제한한다.
- Alloy가 Docker JSON 로그와 존재하는 호스트 Nginx 로그를 Loki로 전송하고, Grafana에서 LogQL로 검색한다. Grafana는 SSH 터널로만 접근한다.
- Loki는 `/var/lib/jbnu-sugang-helper/loki`에 filesystem backend로 30일 보존한다. Object Storage 장기 보존과 복구 rehearsal은 별도 bucket/IAM 계약 후 추가한다.
- deploy log는 root 또는 제한된 운영자만 읽을 수 있고, logrotate로 보존 기간/크기를 제한한다.
- 매 배포 전 disk preflight를 수행한다. 여유 공간 부족 시 자동 prune하지 않고 운영자가 원인을 확인한다.
- OS 보안 업데이트는 자동 적용하되 재부팅은 수동 승인한다. Docker/Compose 업데이트는 maintenance 중 수동으로 적용하고 ARM64·healthcheck를 확인한다.
- 매월 SSH key/GHCR token/runtime secret의 rotation 필요성을 검토한다. rotation은 새 값 추가 → 테스트 → 이전 값 폐기 순서다.
- 운영 rollback rehearsal은 정상 이전 SHA로 갔다가 현재 정상 SHA로 돌아오는 왕복만 수행한다. 의도적으로 깨진 이미지는 production에 배포하지 않는다.

## 10. 알려진 제한과 후속 이슈

- DB backup/restore, PITR, 외부 백업 저장소는 구현하지 않는다. 이 요구가 생기면 별도 설계와 [기존 disaster recovery 정책](../disaster-recovery-policy.md)의 정합성 검토가 필요하다.
- MySQL·Redis Compose `latest`는 재현 가능한 infra rollback을 제공하지 않는다. 장애가 반복되면 version tag/digest pinning으로 전환한다.
- 단일 OCI 서버와 host Nginx는 load balancer가 아니다. 서버 장애 시 전체 API가 중단된다.
- E2E, Prometheus/Alertmanager metric 알림, Slack 알림은 초기 필수 gate가 아니다. Loki 로그 검색은 production observability profile의 일부다.

GitHub branch protection과 저장소 Actions secrets의 수동 설정은 [GitHub 설정 체크리스트](./github-settings.md)를 따르고, 신규 전환·재구축 기록은 [cutover 체크리스트](./cutover-checklist.md)에 남긴다.
