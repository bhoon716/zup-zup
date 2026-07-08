# 네비게이션바 로딩 개선 구현 상세 Grill 세션

- 날짜: 2026-07-03
- 케이스: technical-design-grill.md (구현 상세)
- 스킬 설치 루트: /Users/bhoon/Project/jbnu-sugang-helper/.agents/skills/ultra-grill-me
- 상태: 진행 중

## 요약

[세션 종료 시 작성]

## 질문 로그

### Q1. 백엔드(Spring Boot)에서 `is_logged_in` 쿠키를 어떤 방식으로 발급하고 삭제할까요?

- 막힌 결정: Spring Boot 백엔드 소스 코드 내에서 `is_logged_in` 쿠키를 발급 및 삭제하는 메서드의 책임 부여와 구현 구조가 확정되지 않았습니다.
- 제공된 옵션:
  - 1. (추천) 기존 `AuthService`의 Refresh Token 쿠키 처리 메서드 내에 통합: `addRefreshTokenCookie` 및 `deleteRefreshTokenCookie` 메서드 내부에서 `is_logged_in` 쿠키도 함께 빌드하여 `HttpHeaders.SET_COOKIE` 헤더로 동시 주입.
  - 2. 독립적인 쿠키 처리 메서드 및 명시적 호출: `AuthService`에 `addIsLoggedInCookie` / `deleteIsLoggedInCookie` 등의 전용 메서드를 선언하고, 컨트롤러/서비스 계층에서 개별 호출.
  - 3. 다른 옵션 더 추천받기
  - 4. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: 기존 Refresh Token 처리 방식과 발급/삭제 수명주기를 완벽하게 결합하기 위해 `AuthService` 내부의 기존 메서드들에 `is_logged_in` 발급/삭제 로직을 통합하기로 확정함.
- 새로 확정된 결정:
  - `AuthService.java`의 `addRefreshTokenCookie`에서 `is_logged_in` 쿠키(httpOnly=false, secure=true, maxAge=30일)를 추가 발급한다.
  - `deleteRefreshTokenCookie`에서 `is_logged_in` 쿠키(maxAge=0)를 동시 만료시킨다.
- 남은 열린 질문:
  - 프론트엔드(Next.js)에서의 쿠키 확인 및 상태 갱신 로직 (Q2)
  - 클라이언트-서버 간 불일치 발생 시의 에러 핸들러 연동 (Q3)

### Q2. 프론트엔드(Next.js)에서 `is_logged_in` 쿠키의 존재 여부를 판별하여 스켈레톤 노출을 분기하는 코드를 어디에 작성할까요?

- 막힌 결정: 프론트엔드 단에서 쿠키를 판독하여 로딩 상태(`isLoading`)를 덮어쓰거나 무시하는 조건부 렌더링 로직의 배치 위치가 확정되지 않았습니다.
- 제공된 옵션:
  - 1. (추천) `NavLinks` 컴포넌트 내부에서 판별: `nav-links.tsx` 내에서 `document.cookie`나 쿠키 파서 유틸리티를 활용하여 `is_logged_in` 쿠키 존재 여부를 파악한 뒤, `isLoading && hasLoggedInCookie` 일 때만 스켈레톤을 노출하고, `isLoading`이지만 쿠키가 없을 때는 스켈레톤을 생략하고 즉시 일반 링크만 렌더링.
  - 2. `Header` 상위 컴포넌트에서 판별하여 Props 전달: `header.tsx` 내에서 쿠키 힌트를 읽어 `shouldShowSkeleton` 파생 상태를 계산하고, 이를 `NavLinks`에 prop으로 주입.
  - 3. 다른 옵션 더 추천받기
  - 4. 직접 답변
- 답변: 2
- 결과: 확정
- 해석 결과: 비즈니스/로직 결정을 상위 컨테이너(`Header`)에 위임하여 하위 UI 컴포넌트(`NavLinks`)의 순수성을 보존하고 관리 포인트를 상위로 일원화하기로 확정함.
- 새로 확정된 결정:
  - `Header.tsx` 컴포넌트 내부에서 `hasMounted && document.cookie.includes("is_logged_in=")` 조건 등으로 힌트 존재 여부를 체크한다.
  - 이 힌트 상태와 `isLoading` 상태를 조합해 `shouldShowSkeleton = isLoading && hasLoggedInCookie` 값을 산출한 뒤, `NavLinks` 컴포넌트에 prop으로 전달한다.
- 남은 열린 질문:
  - 클라이언트-서버 간 불일치 발생 시의 에러 핸들러 및 로그아웃 연동 (Q3)

### Q3. 로그아웃 또는 세션 만료 시 브라우저 측에서 `is_logged_in` 쿠키를 명시적으로 파괴하는 클라이언트 코드를 어디에 작성할까요?

- 막힌 결정: 세션 종료 흐름 시 힌트 쿠키 삭제를 담당할 프론트엔드 모듈의 역할 범위가 확정되지 않았습니다.
- 제공된 옵션:
  - 1. (추천) `useAuthStore`의 `logout()` 액션 내부에 통합: `useAuthStore.ts`의 `logout()` 함수가 호출될 때 `document.cookie` 만료 코드를 삽입하여 로그인 세션이 끊기는 모든 케이스에서 쿠키 유실 없이 동기화 삭제되도록 함.
  - 2. Axios HTTP 인터셉터 및 API 클라이언트 내부에서 명시적 삭제: `client.ts`에서 API 401 갱신 실패가 일어나거나 로그아웃 API가 완료되는 시점에 직접 쿠키 삭제 함수를 호출. Zustand 전역 상태 저장소에 DOM API(document) 의존성 유입을 방지함.
  - 3. 다른 옵션 더 추천받기
  - 4. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: 클라이언트 세션 해제 이벤트의 모든 진입점이 useAuthStore의 logout() 액션을 경유하므로, 이곳에 쿠키 만료 코드를 통합하여 유실 없는 완벽한 상태 정리를 보장하기로 확정함.
- 새로 확정된 결정:
  - `useAuthStore.ts` 내 `logout` 스토어 메서드에 `document.cookie` 만료 로직을 추가하여, 로그아웃 호출 시 힌트 쿠키가 즉각 지워지도록 한다.

## 확정된 결정

- **백엔드 AuthService의 통합 관리**: `AuthService.java`의 `addRefreshTokenCookie` 및 `deleteRefreshTokenCookie`에서 `is_logged_in` 쿠키를 동시 발급 및 만료시킨다.
- **Header 컴포넌트의 상위 흐름 제어**: `Header.tsx`에서 쿠키 판독 및 `shouldShowSkeleton`을 계산하여 `NavLinks`에 prop으로 전달하도록 구현한다.
- **클라이언트 전역 스토어에서의 쿠키 파괴**: `useAuthStore.ts`의 `logout()` 스토어 액션 내부에서 힌트 쿠키를 강제 삭제하여 상태 동기화 누수를 원천 방지한다.

## 임시 가정

- `is_logged_in` 쿠키의 만료 시간은 기존 Refresh Token과 똑같이 `30일`(2592000초)로 설정하여, 두 쿠키가 자연스럽게 함께 소멸하거나 갱신되도록 보장함.

## 남은 known unknowns

- 없음.

## 최종 정리

### 1. 정리된 방향
백엔드(`AuthService.java`)에서 Refresh Token과 `is_logged_in` 쿠키를 패키지로 발급/소멸시키는 흐름을 강제하고, 프론트엔드에서는 전역 상태 스토어(`useAuthStore.ts`)의 로그아웃 흐름에 쿠키 삭제를 연동하며, 레이아웃 계산은 `Header.tsx`에서 중앙 통제하는 방식으로 견고한 아키텍처를 구현합니다.

### 2. 확정된 결정
- **백엔드**: `AuthService.java` 내의 기존 쿠키 유틸리티 메서드 수정 (`addRefreshTokenCookie`, `deleteRefreshTokenCookie`).
- **프론트엔드 전역**: `useAuthStore.ts`의 `logout` 액션 내에 쿠키 만료 로직 결합.
- **프론트엔드 UI**: `Header.tsx`에서 쿠키 판정 후 `shouldShowSkeleton` 파생 변수를 `NavLinks`에 전달.

### 3. 아직 남은 known unknowns
- 없음.

### 4. 핵심 가정
- `is_logged_in` 쿠키의 수명과 삭제 방식이 백엔드/프론트엔드 전방위적으로 Refresh Token과 온전히 대칭(1:1 결합)되므로, 상태 불일치가 일어날 확률이 극히 희박함.

### 5. 가장 큰 리스크
- 없음.

### 6. 주요 tradeoff
- Zustand 스토어 내부에 DOM API(`document.cookie`)를 사용하는 것은 아키텍처의 순수성을 일부 양보하지만, 상태 파괴 누락을 예방하는 안전성이 훨씬 높으므로 이를 채택함.

### 7. 하지 않기로 한 것
- Axios 인터셉터 등의 API 클라이언트에서 개별적으로 `document.cookie` 삭제 로직을 흩뿌려 선언하는 것 (책임 분산 방지).

### 8. 가장 작은 다음 행동
- 구현 계획 수립을 위한 [implementation-plan-grill.md](/Users/bhoon/Project/jbnu-sugang-helper/.agents/skills/ultra-grill-me/references/implementation-plan-grill.md) 작성.

### 9. 왜 여기서 멈춰도 되는지
- 백엔드, 프론트엔드 상태 스토어, UI 컴포넌트 전체에 걸쳐 구체적으로 수정할 소스 코드의 위치와 역할 범위가 완전히 명확해졌으므로 검증 완료를 선언함.

"더 이상 고가치 질문이 없습니다. 추가 질문은 대부분 수익 체감 구간으로 보입니다."
