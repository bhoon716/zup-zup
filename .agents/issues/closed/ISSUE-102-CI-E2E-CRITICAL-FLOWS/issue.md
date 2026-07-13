# [P2][ci] OAuth·refresh·첨부파일·알림 핵심 흐름의 E2E/contract coverage 추가

## 문제

현재 web/server 테스트는 단위·service 중심이다. OAuth callback, refresh cookie rotation, 관리자 인증 첨부파일, Web Push/FCM/Discord sender contract, Outbox restart/replay를 실제 HTTP/browser 경계에서 검증하는 E2E가 없다.

## 완료 기준

- Playwright 또는 동등한 browser suite가 login/session refresh/admin feedback attachment를 검증한다.
- provider는 실제 secret 없이 contract/mock server로 status·timeout·retry를 검증한다.
- Outbox crash/restart, DLQ replay, duplicate delivery, unauthorized device delete가 HTTP 통합 테스트로 커버된다.
- CI에서 smoke와 nightly/수동 provider suite를 분리하고 결과를 artifact로 남긴다.

## 근거

- `apps/web`에는 Vitest 중심 테스트만 있고 E2E workflow가 없다.
- `apps/server/src/test/java`에도 provider/outbox의 모든 운영 경계를 browser/HTTP로 묶은 suite가 없다.

## 검증 로그

- Playwright Chromium smoke가 401→refresh→재시도와 관리자 feedback attachment preview를 mock API 경계에서 검증한다.
- PR/수동 browser smoke와 주간/manual server provider·auth·outbox·DLQ·feedback contract suite를 `.github/workflows/e2e.yml`로 분리하고 실패 trace/screenshot/video 및 서버 test result artifact를 보관한다.
- 로컬 `npm run web:build` 및 `npm run web:e2e`는 2개 테스트 모두 통과했다.
