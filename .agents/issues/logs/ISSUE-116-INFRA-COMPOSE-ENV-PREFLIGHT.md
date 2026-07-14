# ISSUE-116 verification log

- 2026-07-14: `cd infra && docker compose up -d`에서 DB·Redis·앱·Alertmanager 관련 환경변수 미설정 경고가 다수 발생하는 것을 확인했다.
- 2026-07-14: Compose가 누락 변수를 빈 문자열로 대체해 후속 장애 원인과 설정 누락을 구분하기 어려운 상태임을 기록했다.
- 2026-07-14: 필수 Compose 보간값에 `:?` fail-fast 검증을 적용하고, 누락 환경을 재현하는 `test-compose-env.sh`를 추가했다.
- 2026-07-14: 환경변수 회귀 검증, Compose 정책, 배포 스모크, 관측성 스모크, deployment alignment 검증이 통과했다.
