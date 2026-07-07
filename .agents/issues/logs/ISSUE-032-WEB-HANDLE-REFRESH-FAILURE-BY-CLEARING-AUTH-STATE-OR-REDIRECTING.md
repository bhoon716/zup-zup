# ISSUE-032-WEB-HANDLE-REFRESH-FAILURE-BY-CLEARING-AUTH-STATE-OR-REDIRECTING

- 2026-07-03T13:13:58+09:00 Created from review finding: refresh failure does not clear auth state or redirect.

- 2026-07-03T13:45:00+09:00 Closed:
  - Added useAuthStore.getState().logout() in Axios response interceptor catch block.
  - Added test case assertion to check if logout is triggered on refresh error.
  - Verified with client.test.ts passing successfully.
