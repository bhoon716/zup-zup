# ISSUE-041-API-STANDARDIZE-RESPONSE-CONTRACTS-AND-PATH-ENCODING

- 2026-07-06T02:46:59+09:00 Created from review finding: API success envelopes, error shapes, and path encoding are inconsistent.
- 2026-07-06T21:07:43+09:00 Standardized selected controller and client success responses around `CommonResponse`, kept intentional no-body endpoints as `204`, and encoded path variables for wishlist and timetable requests.
- 2026-07-06T21:07:43+09:00 Verified `./gradlew testClasses` in `server` and `npm test -- --run src/features/feedback/hooks/useFeedback.test.tsx src/features/admin/hooks/useDday.test.tsx` in `web`.
- 2026-07-06T21:10:04+09:00 Updated security handlers to emit `ErrorResponse` for 401/403 cases and added regression tests for both handlers.
- 2026-07-06T21:10:04+09:00 Re-ran `./gradlew test --tests bhoon.sugang_helper.common.security.exception.CustomAccessDeniedHandlerTest --tests bhoon.sugang_helper.common.security.exception.CustomAuthenticationEntryPointTest`; both tests passed.
