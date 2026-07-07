# ISSUE-042-WEB-FIX-FRONTEND-BOUNDARY-LINT-AND-ACCESSIBILITY-DRIFT

- 2026-07-06T02:46:59+09:00 Created from review finding: frontend shared boundaries are violated, lint is red, and some interactive controls have accessibility drift.
- 2026-07-06T17:58:43+09:00 Fixed shared/api auth failure handling by removing feature-store imports from the shared client, registering the auth failure handler from app providers, adding accessible names to icon-only controls, and correcting custom schedule edit state.
- 2026-07-06T17:58:43+09:00 Verified with `npm run lint` and `npm test -- --run src/shared/api/client.test.ts src/app/providers.test.tsx src/widgets/header/header.test.tsx src/features/admin/components/admin-schedule-panel.test.tsx 'src/app/(main)/announcements/[id]/page.test.tsx'`.
