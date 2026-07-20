# GitHub 저장소 설정 체크리스트

이 문서는 workflow가 저장소 보호 설정을 자동으로 만들지 못하므로, 관리자가 GitHub UI에서 한 번 설정해야 하는 항목을 고정한다.

## `main` branch protection

- main 직접 push 차단
- pull request merge만 허용
- required status check: `CI`
- 리뷰 승인 요구 없음
- merge queue와 up-to-date 요구 없음
- force push/delete branch는 저장소 기본 보호 정책에 맞춰 금지
- E2E workflow는 필수 check로 추가하지 않음. 수동/주기 workflow로만 실행

## Actions secrets and variables

저장소 `Settings → Secrets and variables → Actions`의 `Repository secrets`에 다음 네 값을 등록한다. CD workflow는 Environment를 사용하지 않는다.

- `SERVER_HOST`: SSH로 접속할 OCI reserved IP 또는 hostname
- `SERVER_USER`: SSH 접속 사용자. 현재는 OCI의 `ubuntu`
- `SSH_PRIVATE_KEY`: `SERVER_USER`의 `authorized_keys`에 등록한 SSH private key
- `SERVER_DOTENV`: 기존 `apps/server/.env` 파일 전체 내용. workflow가 OCI의 `/home/ubuntu/jbnu-sugang-helper/apps/server/.env`에 설치한다.

OCI Compose runtime 값은 `/home/ubuntu/jbnu-sugang-helper/.env.runtime`에 운영자가 한 번 입력한다. GHCR token은 별도 secret으로 저장하지 않고, CD가 단기 `GITHUB_TOKEN`으로 원격 login/logout한다.

## Actions 권한

- workflow 기본 권한은 `contents: read`
- Production CD job은 이미지 push와 원격 pull을 위해 `packages: write`를 사용한다.
- `GITHUB_TOKEN`을 로그에 출력하지 않음
- GitHub Actions concurrency는 사용하지 않고, Ubuntu deploy script의 server-side `flock`으로 accidental overlap을 거부
- CD는 `main` push 또는 수동 SHA 입력으로 시작
