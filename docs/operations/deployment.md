# 운영 배포 runbook

> 현재 배포 경로는 1인 운영용 Ubuntu SSH-only 구성이다. GitHub Actions는 기존 네 개 Repository secret만 사용하고, OCI에는 별도 deploy wrapper나 GHCR read-only token을 설치하지 않는다.

## 0. 경로와 상태

```text
SERVER_USER=ubuntu
RELEASE_ROOT=/home/ubuntu/jbnu-sugang-helper
STAGING_ROOT=/home/ubuntu/jbnu-sugang-helper/.staging
RUNTIME_ENV=/home/ubuntu/jbnu-sugang-helper/.env
APP_ENV=/home/ubuntu/jbnu-sugang-helper/apps/server/.env
APP_IMAGE=ghcr.io/<owner>/<repository>/server
IMAGE_TAG=<40자리 commit SHA>
```

- `SERVER_DOTENV`는 앱 전용 `apps/server/.env` 내용이며 배포 시 `APP_ENV`에 설치한다.
- root `.env`에는 Compose용 DB·Redis·named volume·image·Firebase·관측 경로를 저장한다. 실제 secret은 Ubuntu 운영 파일에만 둔다.
- `apps/server/.env`에는 앱 전용 설정·secret만 저장한다.
- 앱 rollback은 이전 SHA를 같은 수동 배포 workflow에 입력하는 방식이다. DB migration은 자동 rollback하지 않는다.

## 1. 최초 Ubuntu bootstrap

이 단계만 운영자가 서버에서 한 번 수행한다.

```bash
sudo usermod -aG docker ubuntu
install -d -m 0750 \
  /home/ubuntu/jbnu-sugang-helper \
  /home/ubuntu/jbnu-sugang-helper/.staging
install -d -m 0700 \
  /home/ubuntu/jbnu-sugang-helper/secrets
if [ ! -e /home/ubuntu/jbnu-sugang-helper/.env ]; then
  install -m 0600 /dev/null /home/ubuntu/jbnu-sugang-helper/.env
fi
```

`ubuntu`로 다시 로그인한 뒤 Docker와 Compose를 확인한다.

```bash
docker info
docker compose version
```

root `.env`에 다음 종류의 값을 채운다.

- `APP_IMAGE_NAME=ghcr.io/<owner>/<repository>/server`
- `FLYWAY_IMAGE=flyway/flyway@sha256:<digest>`
- `DB_*`, `REDIS_*`, `DB_VOLUME_NAME`, `APP_*`, `LOKI_*`, `ALLOY_*`, `GRAFANA_*`, `PROMETHEUS_*`, `TZ`
- `FIREBASE_CONFIG_PATH=/home/ubuntu/jbnu-sugang-helper/secrets/firebase-key.json`

Firebase 파일은 다음 경로에 별도로 설치한다.

```bash
install -o ubuntu -g ubuntu -m 0600 firebase-key.json \
  /home/ubuntu/jbnu-sugang-helper/secrets/firebase-key.json
```

GitHub Actions는 배포 직전에 단기 `GITHUB_TOKEN`으로 GHCR에 로그인하고 종료 시 logout한다. OCI에 GHCR token 파일이나 `install-oci-wrappers.sh`를 만들지 않는다.

## 2. GitHub 설정

`Settings → Secrets and variables → Actions → Repository secrets`에 다음 네 값을 둔다.

- `SERVER_HOST`: OCI IP 또는 hostname
- `SERVER_USER`: `ubuntu`
- `SSH_PRIVATE_KEY`: `ubuntu`의 `authorized_keys`와 짝인 private key
- `SERVER_DOTENV`: `apps/server/.env` 전체 내용

CD는 `main` push 또는 `workflow_dispatch(image_tag)`에서 실행한다. 일반 push는 이미지를 build/push하고, 수동 실행은 기존 SHA 이미지를 pull한다.

## 3. 정상 배포 순서

```text
checkout SHA
  → main push인 경우 JAR build + ARM64 image push
  → release files + apps/server/.env + deploy.sh staging
  → SSH/SCP to Ubuntu
  → transient GITHUB_TOKEN으로 GHCR login
  → docker compose db/redis/Prometheus/Loki/Alloy/Grafana start
  → app image pull
  → 기존 app stop
  → one-shot Flyway migrate
  → app start + health wait
  → internal readiness 확인
  → GHCR logout
  → OCI local DB backup timer는 별도 systemd timer로 매일 실행
```

`Prometheus`, `Loki`, `Grafana Alloy`, `Grafana`는 `observability` profile로 계속 실행한다. Prometheus는 앱 Actuator metrics만 수집하고, 로그 수집기는 Promtail이 아니라 Alloy다.

## 4. 실패 처리

- staging·Compose 검증·infra 시작·image pull 실패: 기존 app을 중지하지 않고 종료한다.
- Flyway 실패: app은 중지 상태로 두며 DB 자동 rollback, `clean`, 자동 `repair`를 수행하지 않는다.
- app start/readiness 실패: 새 app을 중지하고 root `.env`는 변경하지 않는다.
- migration 이후 이전 SHA 재배포는 schema 호환성이 확인된 경우에만 수행한다. 이것은 앱 이미지 재배포이지 DB rollback이 아니다.
- 반복 배포 후 사용하지 않는 SHA 이미지는 운영자가 확인 후 수동 정리한다. 자동 `docker system prune`은 실행하지 않는다.

## 5. 수동 명령

현재 상태와 로그 확인:

```bash
cd /home/ubuntu/jbnu-sugang-helper
docker compose --env-file .env --env-file .env.compose \
  -f docker-compose.yml ps
docker compose --env-file .env --env-file .env.compose \
  -f docker-compose.yml logs --tail=100 app
```

현재 실행 중인 로그 검색은 Grafana를 SSH tunnel로 연다.

```bash
ssh -L 3000:127.0.0.1:3000 ubuntu@<api-host>
```

## 6. 보안·운영 한계

- Ubuntu의 Docker socket 접근은 사실상 root equivalent 권한이다. 이 구성은 단순성을 위해 이를 명시적으로 수용한다.
- SSH host key는 Actions 실행 시 `ssh-keyscan`으로 수집하는 단순 경로다. 고정 fingerprint 검증은 별도 강화 작업이다.
- NPM·DuckDNS는 앱 CD가 관리하지 않는 host 운영 경계다. NPM 설정·인증서는 저장소에 포함하지 않는다.
- MySQL named volume은 backup이 아니며, OCI local DB dump도 host loss backup은 아니다. 서버에 직접 설치하는 `/home/ubuntu/jbnu-sugang-helper/backup-db-local.sh`가 매주 월요일 04:00(`Asia/Seoul`)에 실행되며, off-host backup/Object Storage 이전은 별도 운영 이슈다.
