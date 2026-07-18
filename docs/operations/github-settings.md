# GitHub 저장소·Environment 설정 체크리스트

이 문서는 workflow가 저장소 보호 설정을 자동으로 만들지 못하므로, 관리자가 GitHub UI에서 한 번 설정해야 하는 항목을 고정한다.

## `main` branch protection

- direct push 금지
- pull request merge만 허용
- required status checks:
  - `backend + MySQL + Redis + Flyway`
  - `build ARM64 image (PR)`
- stale approval dismiss와 force push/delete branch는 저장소 정책에 맞춰 비활성화
- E2E workflow는 필수 check로 추가하지 않음. 수동/주기 workflow로만 실행

## `production` Environment

- 사용 가능한 branch를 `main`으로 제한
- SSH private key와 pinned `known_hosts`, staging manifest 서명용 `DEPLOY_MANIFEST_PRIVATE_KEY`를 production Environment에 저장
- OCI runtime secret과 GHCR read-only token은 GitHub secret에 복제하지 않고 OCI root-only file에 저장한다. 설치 시 `GHCR_READ_USERNAME`은 `.env.runtime`, token은 `${RELEASE_ROOT}/secrets/ghcr-read-token`에 둔다.
- 승인 reviewer는 현재 정책상 사용하지 않지만, 운영 규모가 커지면 manual approval을 별도 결정으로 추가

## Actions 권한

- workflow 기본 권한은 `contents: read`
- GHCR push job만 `packages: write`
- `GITHUB_TOKEN`을 로그에 출력하지 않음
- production deploy/rollback workflow는 동일 concurrency group으로 직렬화
- CD는 `PR CI` workflow의 성공적인 `main` 실행(`workflow_run`) 이후에만 시작
