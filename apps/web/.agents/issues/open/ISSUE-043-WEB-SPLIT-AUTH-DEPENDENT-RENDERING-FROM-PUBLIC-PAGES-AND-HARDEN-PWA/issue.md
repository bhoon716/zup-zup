# ISSUE-043-WEB-SPLIT-AUTH-DEPENDENT-RENDERING-FROM-PUBLIC-PAGES-AND-HARDEN-PWA

- Type: bugfix
- State: open
- Workflow: bug-fix
- Created At: 2026-07-06T02:46:59+09:00
- Updated At: 2026-07-06T02:46:59+09:00
- Log: .agents/issues/logs/ISSUE-043-WEB-SPLIT-AUTH-DEPENDENT-RENDERING-FROM-PUBLIC-PAGES-AND-HARDEN-PWA.md
- Workflow State: .agents/issues/open/ISSUE-043-WEB-SPLIT-AUTH-DEPENDENT-RENDERING-FROM-PUBLIC-PAGES-AND-HARDEN-PWA/workflow-state.json

## Goal
Move auth-dependent rendering out of public routes, avoid request-time rendering where not needed, and harden the service worker and PWA asset handling.

## Acceptance Criteria
- Public routes are not forced into request-time rendering by auth-only cookie reads unless that tradeoff is intentional.
- Public detail pages provide the expected server metadata.
- Service worker or PWA URL handling only opens allowed destinations and references correct icons/assets.

## Dependencies
- Next.js app router behavior
- PWA asset inventory

## Evidence Required
- Implementation diff
- Frontend verification output
- Review evidence

## Description
The current frontend mixes auth-dependent rendering into public surfaces and lets the service worker open unvetted URLs. Split those concerns and harden the PWA flow so public pages stay fast and predictable.

## Decisions
- Decisions are stored under `decisions/`.
