# ISSUE-031-SERVER-ENABLE-SECURE-REFRESH-COOKIE-IN-PRODUCTION

- 2026-07-03T13:13:58+09:00 Created from review finding: production refresh cookie is emitted with `secure=false`.

- 2026-07-03T14:10:00+09:00 Closed:
  - Replaced servlet Cookie deletion with Spring ResponseCookie builder inside deleteRefreshTokenCookie.
  - Added new regression unit test to verify secure deletion.
  - Verified with gradlew test and gradle build passing successfully.
