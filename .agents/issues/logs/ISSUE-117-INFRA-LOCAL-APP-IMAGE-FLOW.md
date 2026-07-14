# ISSUE-117 verification log

- 2026-07-14: `cd infra && docker compose up -d`에서 로컬에 없는 `sugang-helper` 앱 이미지를 pull하려다 `pull access denied`로 종료되는 것을 확인했다.
- 2026-07-14: 앱 서비스에 로컬 build 경로가 없고, 운영용 prebuilt image 경로와 실행 절차가 분리되어 있지 않음을 기록했다.
