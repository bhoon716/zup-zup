# ISSUE-117 verification log

- 2026-07-14: `cd infra && docker compose up -d`에서 로컬에 없는 `sugang-helper` 앱 이미지를 pull하려다 `pull access denied`로 종료되는 것을 확인했다.
- 2026-07-14: 앱 서비스에 로컬 build 경로가 없고, 운영용 prebuilt image 경로와 실행 절차가 분리되어 있지 않음을 기록했다.
- 2026-07-15: `infra/.env.example`의 이미지명을 배포 스크립트와 같은 `sugang-helper-app`으로 정렬하고, 로컬 전용 Compose override의 `sugang-helper-app:local` build 경로를 추가했다.
- 2026-07-15: 운영 배포 스크립트가 빌드한 이미지명을 Compose에 명시적으로 전달하도록 수정했다.
- 2026-07-15: local·production image flow regression, Compose policy, deployment safety, observability smoke, deployment alignment 검증과 실제 `sugang-helper-app:local` 이미지 build가 통과했다.
- 2026-07-15: 사용자가 별도 override 없이 `docker compose up -d --build`를 원해 기본 Compose에 서버 Dockerfile build context를 포함하고 local override 파일을 제거했다.
- 2026-07-15: 기본 Compose의 `sugang-helper-app:latest` build와 관련 정책·배포·관측성 검증, 실제 기본 Compose 앱 이미지 build가 통과했다.
