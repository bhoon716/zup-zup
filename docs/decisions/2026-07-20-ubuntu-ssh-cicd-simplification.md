# 결정: Ubuntu SSH-only CI/CD

- 상태: Accepted
- 결정일: 2026-07-20
- 대체 문서: [단일 OCI CI/CD 결정](2026-07-19-single-oci-cicd.md)
- 관련 이슈: [ISSUE-130](../../.agents/issues/in-progress/ISSUE-130-PLATFORM-UBUNTU-SSH-CICD-SIMPLIFICATION/issue.md)

## 결정

1인 운영에 맞춰 CD를 하나의 GitHub Actions job으로 유지한다. `main` push는 ARM64 이미지를 build/push한 뒤 배포하고, `workflow_dispatch(image_tag)`는 기존 SHA를 같은 SSH 경로로 재배포한다.

Actions는 `SERVER_HOST`, `SERVER_USER`, `SSH_PRIVATE_KEY`, `SERVER_DOTENV`만 사용한다. staging 파일과 `deploy.sh`를 Ubuntu에 SCP하고, 원격에서 단기 `GITHUB_TOKEN`으로 GHCR에 로그인한 뒤 Ubuntu 권한으로 배포 script를 실행한다. 배포 종료 시 GHCR logout을 수행한다.

OCI에는 `/usr/local/sbin` wrapper, root libexec, sudoers allowlist, GHCR read-only token 파일을 설치하지 않는다. release root와 staging root는 `ubuntu` 홈 디렉터리 아래(`/home/ubuntu/jbnu-sugang-helper`, `/home/ubuntu/jbnu-sugang-helper-staging`)에 두고, 배포 파일은 SSH 사용자 권한으로 관리한다. Ubuntu는 Docker 그룹 권한을 사용하며 이 권한은 root equivalent임을 운영 문서에 명시한다.

## 배포 순서

```text
staging Compose 검증
  → runtime 파일 반영
  → db/redis/Loki/Alloy/Grafana start + wait
  → app image pull
  → app stop
  → Flyway migrate
  → app start + readiness
  → .env.release 갱신
```

checksum manifest, release history/다중 release 디렉터리, 반복적인 infra image preflight, 별도 rollback script는 제거한다. Loki·Alloy·Grafana, Flyway one-shot migration, 내부 readiness, server-side `flock`은 유지한다.

## 복구와 한계

이전 SHA 수동 재배포는 앱 image 복구 경로다. 이미 적용된 DB migration을 되돌리지 않으며, schema 호환성이 확인되지 않은 이전 app은 재배포하지 않는다. 사용하지 않는 Docker image 정리는 운영자가 확인 후 수동으로 수행한다.
