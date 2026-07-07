# Repository Scorecard

Scores are out of 10 and reflect the audited current state.

| Area | Score | Basis |
| --- | ---: | --- |
| Architecture | 5 | Clear package intent, but backend and frontend boundaries are violated. |
| Security | 3 | Literal production secrets, OAuth state risk, public internal services, sensitive logs. |
| Performance | 5 | Core paths work, but search/crawl/history scaling risks are visible. |
| Maintainability | 5 | Tests exist, but complexity hotspots and API/DTO coupling are significant. |
| Scalability | 5 | Single-host design is coherent; DB/query/history growth needs controls. |
| Reliability | 4 | Migration safety, batch swallowing, event timing, and CI gaps are material. |
| Testability | 5 | Unit coverage is meaningful, but CI, Flyway, request security, and E2E gaps remain. |
| Readability | 6 | Most code is readable, but large pages/services and misleading validation/rate-limit stubs hurt. |
| Observability | 5 | Loki/Promtail/Grafana are present, but exposure, durability, and verification are incomplete. |
| Documentation | 4 | Issue docs exist, but PRD/ARD do not describe the actual whole system. |

Overall: 4.7 / 10
