# 소스 주석 정리

기능 변경 커밋 뒤 별도로 273개 프로젝트 소스 파일의 설명·Javadoc·죽은 코드 주석 1,423개를 제거했다. 문서·vendor/생성물·lockfile·기존 DB migration·Gradle wrapper는 건드리지 않았다. Course 등 과목 필드의 의미·외부 데이터 매핑은 사용자 예외에 따라 보존했다. 일반 일정 DTO 날짜 형식과 JWT 테스트의 단위 설명을 과목 필드 예외로 잘못 확대하지 않았다.

## 검증 방법

JS/TS/JSX는 Babel AST와 비주석 토큰, Java/Groovy/Alloy는 문자열·텍스트블록을 구분하는 lexer의 비주석 토큰, CSS는 PostCSS AST, XML은 트리, YAML은 파싱된 값, Python은 tokenize/AST, shell은 인용부·heredoc 구분 및 bash -n으로 검사했다. 언어 경계 자체 검사 16개를 통과했다. 변경 273개 파일의 SHA가 계획과 일치하고 적용 후 재스캔 changed=0/skip=0, diff 검사도 통과했다. 문자열·URL·정규식·템플릿·YAML block scalar·heredoc 내용은 보존했다. 기존 줄바꿈과 JSX의 빈 표현식 {}는 코드 토큰을 바꾸지 않기 위해 남겼다.

## 남긴 주석 71개

| 위치 | 이유 |
| --- | --- |
| [.agents/scripts/check_markdown_links.py:1](../.agents/scripts/check_markdown_links.py#L1) | shebang |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/request/CourseSearchCondition.java:78](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/request/CourseSearchCondition.java#L78) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:40](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L40) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:43](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L43) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:46](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L46) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:49](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L49) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:52](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L52) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:55](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L55) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:58](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L58) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:61](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L61) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:64](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L64) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:68](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L68) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:71](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L71) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:74](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L74) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:78](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L78) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:81](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L81) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:85](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L85) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:89](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L89) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:92](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L92) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:95](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L95) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:99](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L99) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:102](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L102) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:105](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L105) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:108](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L108) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:111](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L111) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:115](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L115) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:119](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L119) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:122](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L122) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:125](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L125) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:128](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L128) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:131](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L131) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:134](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L134) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:137](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L137) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java:140](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java#L140) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java:8](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java#L8) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java:9](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java#L9) | field meaning / external mapping |
| [apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java:10](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java#L10) | field meaning / external mapping |
| [apps/web/src/app/%28admin%29/admin/feedbacks/page.tsx:214](../apps/web/src/app/%28admin%29/admin/feedbacks/page.tsx#L214) | license/generated/toolchain/analysis directive |
| [apps/web/src/app/%28admin%29/admin/notification-deliveries/page.tsx:107](../apps/web/src/app/%28admin%29/admin/notification-deliveries/page.tsx#L107) | empty block comment preserves lint behavior |
| [apps/web/src/app/%28user%29/onboarding/page.tsx:175](../apps/web/src/app/%28user%29/onboarding/page.tsx#L175) | license/generated/toolchain/analysis directive |
| [apps/web/src/app/%28user%29/settings/useSettingsPage.ts:199](../apps/web/src/app/%28user%29/settings/useSettingsPage.ts#L199) | license/generated/toolchain/analysis directive |
| [apps/web/src/app/providers.tsx:132](../apps/web/src/app/providers.tsx#L132) | license/generated/toolchain/analysis directive |
| [apps/web/src/app/sitemap.ts:78](../apps/web/src/app/sitemap.ts#L78) | empty block comment preserves lint behavior |
| [apps/web/src/features/auth/components/login-card.tsx:22](../apps/web/src/features/auth/components/login-card.tsx#L22) | license/generated/toolchain/analysis directive |
| [apps/web/src/features/auth/store/useAuthStore.ts:22](../apps/web/src/features/auth/store/useAuthStore.ts#L22) | empty block comment preserves lint behavior |
| [apps/web/src/features/course/components/course-guide-modal.tsx:63](../apps/web/src/features/course/components/course-guide-modal.tsx#L63) | empty block comment preserves lint behavior |
| [apps/web/src/features/course/components/time-table-selector.tsx:233](../apps/web/src/features/course/components/time-table-selector.tsx#L233) | empty block comment preserves lint behavior |
| [apps/web/src/shared/hooks/useHasMounted.ts:11](../apps/web/src/shared/hooks/useHasMounted.ts#L11) | license/generated/toolchain/analysis directive |
| [apps/web/src/shared/lib/navigation.ts:5](../apps/web/src/shared/lib/navigation.ts#L5) | license/generated/toolchain/analysis directive |
| [apps/web/src/shared/telemetry/client-error.ts:130](../apps/web/src/shared/telemetry/client-error.ts#L130) | empty block comment preserves lint behavior |
| [apps/web/src/widgets/auth/login-modal.tsx:23](../apps/web/src/widgets/auth/login-modal.tsx#L23) | license/generated/toolchain/analysis directive |
| [infra/scripts/backup-server-export.sh:1](../infra/scripts/backup-server-export.sh#L1) | shebang |
| [infra/scripts/backup-to-macbook.sh:1](../infra/scripts/backup-to-macbook.sh#L1) | shebang |
| [infra/scripts/compose-observability-diagnostics.sh:1](../infra/scripts/compose-observability-diagnostics.sh#L1) | shebang |
| [infra/scripts/deploy-release.sh:1](../infra/scripts/deploy-release.sh#L1) | shebang |
| [infra/scripts/prepare-app-host-directories.sh:1](../infra/scripts/prepare-app-host-directories.sh#L1) | shebang |
| [infra/scripts/test-app-env-preflight.sh:1](../infra/scripts/test-app-env-preflight.sh#L1) | shebang |
| [infra/scripts/test-backup-to-macbook.sh:16](../infra/scripts/test-backup-to-macbook.sh#L16) | heredoc payload is executable/data content; preserved |
| [infra/scripts/test-backup-to-macbook.sh:1](../infra/scripts/test-backup-to-macbook.sh#L1) | shebang |
| [infra/scripts/test-cd-workflow-contract.sh:1](../infra/scripts/test-cd-workflow-contract.sh#L1) | shebang |
| [infra/scripts/test-local-app-image-flow.sh:1](../infra/scripts/test-local-app-image-flow.sh#L1) | shebang |
| [infra/scripts/test-local-compose.sh:1](../infra/scripts/test-local-compose.sh#L1) | shebang |
| [infra/scripts/test-observability-contract.sh:1](../infra/scripts/test-observability-contract.sh#L1) | shebang |
| [infra/scripts/test-observability-smoke.sh:1](../infra/scripts/test-observability-smoke.sh#L1) | shebang |
| [infra/scripts/test-redis-auth-state-recovery.sh:1](../infra/scripts/test-redis-auth-state-recovery.sh#L1) | shebang |
| [infra/scripts/test-release-layout-contract.sh:1](../infra/scripts/test-release-layout-contract.sh#L1) | shebang |
| [infra/scripts/test-runtime-contract.sh:1](../infra/scripts/test-runtime-contract.sh#L1) | shebang |
| [infra/scripts/test-uptime-contract.sh:1](../infra/scripts/test-uptime-contract.sh#L1) | shebang |
| [infra/scripts/verify-compose-policy.sh:1](../infra/scripts/verify-compose-policy.sh#L1) | shebang |
| [infra/scripts/verify-log-policy.sh:1](../infra/scripts/verify-log-policy.sh#L1) | shebang |
| [infra/scripts/verify-mac-restore.sh:1](../infra/scripts/verify-mac-restore.sh#L1) | shebang |

Course.java 32개·CourseSearchCondition 1개·NotificationTarget 연동 필드 3개, 실제 린터 지시문 8개, 빈 블록 lint 의미 보존 6개, shebang 20개와 heredoc 안 shebang 1개다. 예외를 이유로 일반 설명 주석을 모두 유지하지 않았다. 빈 catch 설명 6개는 제거 시 no-empty 같은 규칙의 동작이 바뀌므로 보존했다.
