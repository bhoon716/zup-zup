# [P2][web] 전체 코드 lint 경고 제거

## 문제

현재 `npm run web:lint`가 React 내부 이동과 OAuth/PWA 전체 문서 이동을 구분하지 못해 `window.location.href`/`assign`을 사용하는 8개 경고를 출력한다. Spring 쪽은 Checkstyle/PMD 위반은 없지만 OAuth 테스트의 Mockito generic 추론에서 Java 컴파일러 unchecked advice가 발생하므로 함께 제거한다.

## 완료 기준

- web lint가 경고 0건으로 통과한다.
- 서버 `./gradlew check --no-daemon`의 Checkstyle/PMD/test가 계속 통과한다.
- 기존 redirect 경로, 인증 실패 처리, 로그아웃/온보딩 이동 동작을 보존한다.

## 구현 메모

- 로그아웃·탈퇴 후 `/login` 이동은 `useRouter().replace()`를 사용한다.
- OAuth·Discord·PWA 알림 이동은 SPA 라우팅이 아닌 전체 문서 이동이므로, 필요한 `window.location` 호출에만 ESLint 예외 사유를 남긴다.

## 근거

- `apps/web/src/app/(user)/onboarding/page.tsx`
- `apps/web/src/app/(user)/settings/useSettingsPage.ts`
- `apps/web/src/app/providers.tsx`
- `apps/web/src/features/auth/components/login-card.tsx`
- `apps/web/src/features/user/hooks/useUser.ts`
- `apps/web/src/shared/lib/navigation.ts`
- `apps/web/src/widgets/auth/login-modal.tsx`
