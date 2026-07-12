# Web 의존성 운영 기준

시행: 2026-07-13. 이 문서는 `ISSUE-104`의 운영 계약이다.

## 기준 버전

- production은 Next.js stable을 기본으로 사용한다. 단, 현재 stable `16.2.10`이 취약한 PostCSS를 직접 고정하므로, 해당 보안 수정이 stable에 반영될 때까지 `next`와 `eslint-config-next` `16.3.0-preview.5`를 임시 production 예외로 고정한다. React/React DOM은 `19.2.7`, Node.js는 `22.x`다.
- 단일 기준 lockfile은 루트 `package-lock.json`이다. `apps/web`은 npm workspace이므로 하위 lockfile을 만들지 않으며, 설치·CI는 루트에서 `npm ci`를 사용한다.
- root는 hoist되는 Next·`eslint-config-next`의 peer를 안정적으로 해석하도록 React/React DOM과 `eslint`·`typescript`를 같은 고정 버전으로 선언한다. 이 도구와 runtime peer를 workspace 아래로만 두면 clean install 뒤 test 또는 lint가 깨질 수 있다.
- Vercel도 root와 `apps/web/package.json`의 `engines.node: 22.x`를 따라 CI와 같은 major runtime을 사용한다. Vercel은 해당 major의 minor/patch 보안 업데이트를 자동 적용한다.

## 보안 예외

Next `16.2.10`은 내부적으로 취약한 PostCSS `8.4.31`을 선언한다. npm workspace lockfile에서 이를 안전하게 override할 수 없음을 확인했으므로, PostCSS `8.5.10`을 직접 포함한 `16.3.0-preview.5`를 사용한다. [PostCSS advisory](https://github.com/advisories/GHSA-qx2v-qp2m-jg93)가 영향을 받는 `< 8.5.10` 범위를 벗어나는지 `npm audit`로 매주 확인한다.

`@emoji-mart/react`의 published peer 범위는 React 18까지만 표기하지만, 현재 React 19 경로는 UI 테스트와 build로 검증한다. 전역 `legacy-peer-deps`는 peer 배치를 깨뜨릴 수 있어 사용하지 않는다. 이 picker를 유지한 dependency 갱신은 검토 후 `npm install --package-lock-only --force`로 lockfile을 만들고, 반드시 clean `npm ci`, emoji picker test, lint, build를 다시 통과시킨다. React를 올리거나 picker를 교체할 때 이 예외 자체를 제거한다.

## 정기 점검과 전환

1. 매주 월요일 03:17 UTC에 `web-dependency-audit.yml`이 production dependency audit과 outdated 보고를 실행한다. high 이상 취약점은 실패로 남긴다.
2. stable minor/patch 전환 전에는 루트에서 `npm ci`, `npm audit --omit=dev`, test, lint, build와 OAuth·API rewrite·인증 첨부 경로 smoke를 실행한다.
3. upstream stable Next가 PostCSS `8.5.10` 이상을 직접 포함하면 preview 예외를 제거하고 stable로 전환한 뒤 다시 audit한다. 새 preview/canary는 stable 전환 전까지 production에 넣지 않는다.
4. Vercel Preview에서 smoke를 마친 뒤 production으로 승격한다. 장애 시 Deployments 화면에서 직전 production으로 Instant Rollback하고, 원인을 고친 뒤 새 deployment를 Promote한다.

## 근거

- [Next 16.2.10 release](https://github.com/vercel/next.js/releases/tag/v16.2.10)
- [Next support policy](https://nextjs.org/support-policy)
- [Vercel Node.js versions](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions)
