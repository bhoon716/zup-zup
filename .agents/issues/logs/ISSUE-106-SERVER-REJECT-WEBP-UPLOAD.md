# ISSUE-106 verification log

- 2026-07-14: Red phase confirmed the old behavior: the server accepted a static WebP upload and the web compressor emitted `image/webp`.
- 2026-07-14: Feedback uploads now accept only JPEG/PNG MIME and extensions; WebP is rejected before decoding. The browser file input and client compressor use JPEG/PNG only, and unsupported source types are rejected before compression.
- 2026-07-14: Removed TwelveMonkeys `imageio-webp`, WebP decoder startup verification, and WebP-specific sanitizer branch. Legacy stored WebP files remain download-only (`application/octet-stream`) and are not newly accepted.
- 2026-07-14: `./gradlew check --no-daemon` passed; web Vitest passed (61 files/183 tests), lint passed with 8 pre-existing warnings, production build passed, and Playwright smoke passed (2 tests).
