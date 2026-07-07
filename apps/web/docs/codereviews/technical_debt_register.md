# Technical Debt Register

| ID | Debt | Impact | Paydown Strategy | Priority |
| --- | --- | --- | --- | --- |
| TD-001 | Application services depend on presentation DTOs | High coupling | Add application command/result layer module by module | P1 |
| TD-002 | Frontend shared imports feature state | Lint failure, cycle | Invert auth failure callback registration | P1 |
| TD-003 | Search repository suppresses complexity | Search change risk | Extract predicate/sort builders | P2 |
| TD-004 | NotificationService centralizes channel policy | Channel-change risk | Add channel handler strategy | P2 |
| TD-005 | Settings/timetable route pages are large orchestration components | UI change conflicts | Extract hooks and section components | P3 |
| TD-006 | API response/error shapes vary | Client drift | Standardize contracts and tests | P2 |
| TD-007 | Infra verification script under-covers log state requirements | Ops drift | Expand checks for mounts/positions/backup | P2 |
| TD-008 | Project docs do not describe actual stack | Review/deploy ambiguity | Add project-level architecture docs | P3 |
