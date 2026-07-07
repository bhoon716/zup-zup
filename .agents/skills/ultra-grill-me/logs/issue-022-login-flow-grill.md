# ISSUE-022 Login Flow Grill Session

- 날짜: 2026-06-29
- 케이스: technical-design-grill
- 상태: 진행 중

## 요약

OAuth2 성공 시 access token 전달 방식과 세션 제거 범위를 확정해야 하는 상태입니다.

## 질문 로그

### Q1. OAuth2 성공 직후 access token을 어떻게 전달할까요?

- 막힌 결정: 서버가 OAuth2 성공 시 access token까지 즉시 전달할지, 아니면 refresh cookie만 심고 프론트가 `/reissue`로 access token을 가져가게 할지 정해야 합니다.
- 답변: "원래는 access token은 헤더, refresh token은 쿠키가 베스트 아닌가?"라는 방향성 질문
- 결과: 확정

### Q2. auth 플로우에서 HttpSession을 완전히 제거할까요?

- 막힌 결정: OAuth2 로그인과 토큰 갱신 경로를 stateless로 강제할지, 호환성을 위해 일부 세션 사용을 남길지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q3. OAuth2 성공 시 refresh cookie를 어떤 속성으로 심을까요?

- 막힌 결정: silent login이 cross-domain에서도 동작하면서도 쿠키가 과도하게 노출되지 않도록 `SameSite`, `Secure`, `Path`, `HttpOnly` 정책을 정해야 합니다.
- 답변: 추천 방안으로
- 결과: 확정

## 2026-06-29T00:00:00Z

- Notes:
  - User asked what `SameSite=Lax` means.
  - This needs to be clarified before choosing the cookie policy because it affects whether the refresh cookie will be sent on cross-site reissue requests.

## 확정된 결정

- 없음
- access token 전달은 `refresh cookie only -> /reissue` 경로로 진행
- auth 경로에서 `HttpSession` 완전 제거
- refresh cookie 속성은 `HttpOnly + Secure + SameSite=Lax + Path=/`
- 로그인 redirect 착지 후 즉시 자동 `/reissue`
- `/reissue` 실패 시 로그인 화면으로 즉시 이동

### Q4. 프론트는 언제 `/reissue`를 호출할까요?

- 막힌 결정: OAuth2 redirect 직후 자동 재발급할지, 로그인 완료 화면에서 수동/조건부로 재발급할지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q5. `/reissue`가 실패하면 첫 진입 화면은 어떻게 처리할까요?

- 막힌 결정: 자동 재발급이 실패했을 때 사용자를 로그인 화면으로 보낼지, 오류 화면을 보여줄지, 재시도 유도 화면을 둘지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q6. 기존 세션/구버전 토큰은 어떻게 마이그레이션할까요?

- 막힌 결정: 배포 직후 기존 사용자 세션과 세션에 저장된 토큰을 즉시 끊을지, 짧은 유예 기간을 둘지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q7. `/reissue`가 돌려준 access token을 프론트가 어디에 저장할까요?

- 막힌 결정: access token을 메모리 상태로만 둘지, 다른 저장소를 쓸지 정해야 실제 API 호출 방식이 결정됩니다.
- 답변: 1
- 결과: 확정

### Q8. 페이지 새로고침이나 직접 진입 시 access token은 어떻게 복구할까요?

- 막힌 결정: 메모리 저장만 선택하면 새로고침 후 토큰이 사라지므로, 앱 부트스트랩에서 자동 `/reissue`를 다시 할지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q9. 여러 탭에서 access token 상태를 어떻게 맞출까요?

- 막힌 결정: 한 탭에서 로그인/로그아웃/reissue가 일어날 때 다른 탭의 메모리 상태를 맞출지, 탭마다 독립적으로 둘지 정해야 합니다.
- 답변: 탭 독립
- 결과: 확정

### Q10. 한 탭에서 로그아웃했을 때 다른 탭은 어떻게 할까요?

- 막힌 결정: 탭 독립을 선택하면 다른 탭의 access token은 즉시 알 수 없으므로, 로그인 상태를 유지할지 다음 API 실패 때 정리할지 정해야 합니다.
- 답변: 1
- 결과: 확정

### Q11. `/reissue`는 access token을 어떤 형식으로 반환할까요?

- 막힌 결정: 프론트가 메모리에 저장할 access token을 JSON body로 받을지, 응답 헤더로 받을지 정해야 합니다.
- 답변: 대기 중
- 결과: -

## 2026-06-29T22:58:07+09:00

- Notes:
- Recommended default: keep tabs independent.
- Reason: access token is memory-only, so cross-tab sync would require extra broadcast/session plumbing that weakens the current stateless simplification.

## 2026-06-29T00:00:00Z

- Notes:
- Clarified that the user's question is about the best-practice transport pattern, not a final choice yet.
- The decision still blocks the stateless OAuth2 flow implementation.

## 2026-06-29T22:58:07+09:00

- Notes:
  - Chose automatic `/reissue` immediately after redirect as the silent-login entry point.
  - The remaining blocker is the failure-handling path when `/reissue` does not return an access token.

## 임시 가정

- 없음

## 남은 known unknowns

- OAuth2 성공 직후 access token을 서버가 직접 내려줄지 여부

## 최종 정리

[세션 종료 시 작성]
