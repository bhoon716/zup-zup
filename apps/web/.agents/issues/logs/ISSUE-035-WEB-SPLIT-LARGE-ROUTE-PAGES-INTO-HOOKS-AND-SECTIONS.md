# ISSUE-035-WEB-SPLIT-LARGE-ROUTE-PAGES-INTO-HOOKS-AND-SECTIONS

- 2026-07-06T02:46:59+09:00 Created from review finding: settings and timetable route pages are large orchestration components.
- 2026-07-06T21:25:28+09:00 Split Settings and Timetable route pages into smaller section files while preserving page behavior.
- 2026-07-06T21:25:28+09:00 Verified `npm test -- --run 'src/app/(user)/settings/page.test.tsx' 'src/app/(user)/timetable/page.test.tsx'` and `npm run lint -- 'src/app/(user)/settings/settings-page.tsx' 'src/app/(user)/settings/settings-sections.tsx' 'src/app/(user)/timetable/timetable-page.tsx' 'src/app/(user)/timetable/timetable-main-section.tsx' 'src/app/(user)/timetable/timetable-sidebar-section.tsx'`.
