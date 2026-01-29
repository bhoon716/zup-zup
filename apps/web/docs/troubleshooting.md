# 웹 프론트엔드 트러블슈팅 기록 (Web Troubleshooting)

이 문서는 `web` 모듈 개발 중 발생한 문제와 기술적 해결책을 기록합니다.

---

## 1. Docker 환경에서의 Radix UI 모듈 인식 오류

### 문제 상황

Docker 환경에서 `next dev` 실행 시, `package.json`에 명시된 `@radix-ui/react-alert-dialog`, `@radix-ui/react-tabs` 등의 모듈을 찾을 수 없다는 `Module not found` 에러가 발생하며 빌드가 중단되었습니다. 로컬 환경에서는 정상 작동하나 컨테이너 내부의 `node_modules`에만 특정 패키지가 누락되는 현상이 반복되었습니다.

### 원인

1. **볼륨 마운트 충돌**: 로컬의 `node_modules`와 컨테이너 내의 `node_modules`가 도커 익명 볼륨 설정에 의해 꼬일 수 있음.
2. **Lockfile 불일치**: `package-lock.json`의 변경 사항이 컨테이너 빌드 시점에 완전히 반영되지 않거나 캐시된 이미지를 사용하여 구버전 의존성이 유지됨.

### 해결책

1. **수동 모듈 주입**: 가동 중인 컨테이너 내에서 `docker exec`를 통해 누락된 패키지를 강제로 재설치하여 런타임 오류를 즉시 해결.
   - `docker exec sugang-helper-web npm install @radix-ui/react-alert-dialog @radix-ui/react-tabs`
2. **의존성 잠금 동기화**: 로컬에서 `npm install`을 새로 수행하여 `package-lock.json`을 갱신하고, 이를 Docker 빌드 시 `npm ci` 과정에서 사용하도록 구성.
3. **볼륨 대피(Exclusion)**: `docker-compose.yml`에서 `/app/node_modules`를 익명 볼륨으로 처리하여 컨테이너 내의 패키지가 로컬 환경에 덮어씌워지지 않도록 설정.

### 결과

모든 UI 컴포넌트(`Alert`, `Tabs`, `Switch` 등)가 Turbopack 환경에서 정상적으로 컴파일되고 렌더링됨을 확인했습니다.

---

## 2. API 응답 필드 불일치 (Course/Subscription)

### 문제 상황

백엔드에서 내려오는 데이터 필드명(예: `professor`)과 프론트엔트 인터페이스(`professorName`)가 일치하지 않아 데이터가 `NaN`으로 표시되거나 목록이 비어 보이는 현상이 발생했습니다.

### 해결책

- **규격 통일**: 자체 정의한 **OpenAPI v0** 사양을 기준으로 백엔드 DTO와 프론트엔드 `types/api.ts`를 전면 재매핑했습니다.
- **주요 수정**: `totalSeats`, `currentSeats`, `availableSeats`, `professorName` 등으로 모든 필드명을 표준화했습니다.

### 결과

검색 결과 및 구독 목록에서 강의명, 교수명, 수강 인원 현황이 정확하게 노출됩니다.

---

## 3. 초기 검색 상태 오인 및 로딩 피드백 부재

### 문제 상황

1. **초기 상태 오인**: 검색 페이지 진입 시 검색 조건이 비어있어(`{}`) 아무런 데이터가 뜨지 않았고, 사용자는 이를 "버튼이 동작하지 않음"으로 오인했습니다.
2. **피드백 부재**: 검색 버튼 클릭 시 로딩 표시가 없어 네트워크 지연 시 사용자가 중복 클릭하거나 멈춘 것으로 판단했습니다.

### 해결책

1. **기본값 설정**: `SearchPage`의 초기 상태를 `2026-1학기`로 설정하여 진입 즉시 데이터가 노출되도록 수정했습니다.
2. **스켈레톤 UI 도입**: 데이터 로딩 중 빈 화면 대신 테이블 형태의 **Skeleton Loader**를 보여주어 로딩 중임을 명확히 인지시켰습니다.
3. **버튼 상태 관리**: API 요청 중에는 검색 버튼 내부에 스피너를 표시하고 클릭을 방지(`disabled`)했습니다.

### 결과

사용자가 페이지 진입 시점부터 검색 완료 시점까지 끊김 없는(Seamless) 경험을 하게 되었으며, "고장"이라는 오인을 원천 차단했습니다.

### 4. 테이블 헤더 고정 (Sticky Header) 이슈

- **문제**: `shadcn/ui`의 `Table` 컴포넌트 사용 시, 내부적으로 감싸고 있는 `div`의 `overflow-x-auto` 속성 때문에 `sticky` 포지셔닝이 스크롤 컨테이너 기준이 아닌 내부 div 기준으로 잡혀 헤더가 고정되지 않는 현상 발생.
- **해결**: `CourseTable` 컴포넌트에서 `Table` 래퍼 컴포넌트 대신 표준 HTML `table` 태그를 직접 사용하고, 외부 스크롤 컨테이너(`max-h-[650px]`)에 맞춰 `sticky`가 동작하도록 구조를 변경함.

---

## 5. 검색 경험(UX) 최적화: 조건부 필터 및 자동 접힘

### 문제 상황

1. 상세 검색 필터의 항목이 많아져 실제 검색 결과 리스트가 화면 하단으로 밀려나 가독성이 저하되었습니다.
2. 교양 관련 검색 필터가 전공 검색 시에도 노출되어 혼란을 야기했습니다.

### 해결책

1. **조건부 렌더링**: 이수구분이 '교양'인 경우에만 교양 상세 카테고리 필터가 애니메이션과 함께 나타나도록 수정했습니다.
2. **자동 접힘(Auto-Collapse)**: 검색 버튼을 누르거나 엔터 키를 입력하여 검색이 시작되면 자동적으로 `Collapsible` 필터바가 닫히도록 로직을 추가하여 결과 화면이 즉시 상단에 노출되도록 했습니다.

### 결과

모바일 및 데스크톱 환경 모두에서 화면 공간을 효율적으로 사용하게 되었으며, 사용자의 검색 흐름(Flow)이 비약적으로 매끄러워졌습니다.

---

## 6. 서비스 워커 무한 로딩 및 타임아웃 처리

### 문제 상황

웹푸시 기기 등록 시, `navigator.serviceWorker.ready`가 promise 상태에서 영원히 대기하여 UI가 무한 로딩("등록 중...") 상태에 빠지는 현상이 간헐적으로 발생했습니다.

### 원인

서비스 워커 등록이 어떤 이유로 지연되거나 이미 등록된 워커가 비정상 상태일 때, ready 속성이 resolve 되지 않는 경우가 있었습니다.

### 해결책

- **Time-out 도입**: `Promise.race`를 사용하여 서비스 워커 준비에 5초의 타임아웃을 걸었습니다.
- **Fail-safe**: 5초 내에 응답이 없으면 명확한 에러를 발생시키고 UI 로딩을 해제하여 사용자가 재시도를 할 수 있도록 유도했습니다.

```typescript
// webpush.ts
const registration = await Promise.race([
  navigator.serviceWorker.ready,
  new Promise((_, reject) =>
    setTimeout(() => reject(new Error("Timeout")), 5000),
  ),
]);
```

### 결과

무한 대기 상황이 사라지고, 사용자에게 즉각적인 피드백(성공/실패)을 제공하게 되었습니다.

---

## 7. Web Push VAPID 키 환경 변수 연동

### 문제 상황

웹푸시 구독(`pushManager.subscribe`) 시점에 `applicationServerKey`가 유효하지 않다는 에러가 발생하거나 구독 객체가 `null`로 반환되었습니다.

### 원인

Next.js 환경에서 클라이언트 사이드 코드(브라우저)는 `NEXT_PUBLIC_` 접두사가 붙은 환경 변수만 접근할 수 있는데, `.env` 파일에 접두사 없이 정의하거나 `process.env` 접근이 누락되어 키 값이 `undefined`로 전달되었습니다.

### 해결책

- **변수명 변경**: `.env` 파일의 변수명을 `NEXT_PUBLIC_VAPID_KEY`로 수정했습니다.
- **유효성 검사**: `webpush.ts` 유틸리티에서 키가 존재하는지 검사하는 방어 코드를 추가하여, 키 누락 시 명확한 에러 메시지를 던지도록 수정했습니다.

### 결과

서버와 동일한 공개 키를 사용하여 VAPID 핸드셰이크가 정상적으로 이루어지게 되었습니다.

---

## 8. 포그라운드 알림 미표시 (이중 알림 구조)

### 문제 상황

브라우저 탭을 보고 있는 상태(Foreground)에서는 OS 정책이나 브라우저 설정에 따라 시스템 푸시 알림이 뜨지 않아, 사용자가 중요 알림을 놓칠 가능성이 있었습니다.

### 해결책

서비스 워커와 클라이언트 간의 **양방향 통신(Message Passing)**을 활용한 이중 알림 구조를 구축했습니다.

1. **Service Worker**: 푸시 수신 시 `showNotification`을 호출함과 동시에, 열려 있는 모든 Window Client에게 `postMessage`로 푸시 내용을 전송합니다.
2. **Client**: `navigator.serviceWorker`의 메시지 이벤트를 리스닝하고 있다가, 푸시 메시지가 오면 **`sonner` Toast**를 화면 상단에 띄웁니다.
3. **UX 강화**: `requireInteraction: true` 옵션을 추가하여 사용자가 닫기 전까지 시스템 알림이 유지되도록 설정했습니다.

### 결과

## 사용자가 다른 작업을 하거나 탭을 보고 있을 때 등 모든 시나리오에서 알림을 확실히 인지할 수 있게 되었습니다.

## 9. API 요청 중복 제거 (Request Deduplication) 및 프로미스 캐싱

### 문제 상황

로그인 후 또는 페이지 진입 시 `/api/v1/users/me` API가 짧은 간격으로 2번 이상 호출되는 현상이 발생했습니다. 이는 React Strict Mode의 영향과 더불어, Zustand의 세션 체크와 React Query의 데이터 페칭이 각각 독립적으로 발생하며 네트워크 자원을 낭비하는 결과를 초래했습니다.

### 해결책

API 레이어(`lib/api/user.ts`)에서 **Promise Caching** 패턴을 도입하여 짧은 시간 내 발생하는 동일 요청을 하나로 묶었습니다.

```typescript
let profilePromise: Promise<CommonResponse<User>> | null = null;

export const getMyProfile = async () => {
  if (profilePromise) return profilePromise; // 가고 있는 기차가 있으면 그 티켓을 같이 씀

  profilePromise = (async () => {
    try {
      const { data } = await api.get("/api/v1/users/me");
      return data;
    } finally {
      setTimeout(() => {
        profilePromise = null;
      }, 100); // 처리 완료 후 캐시 해제
    }
  })();
  return profilePromise;
};
```

### 결과

서로 다른 상태 관리 도구(Zustand, React Query)가 동일한 API를 호출하더라도 실제 네트워크 요청은 단 1회만 발생하게 되어 초기 로딩 성능이 개선되고 서버 부하를 줄였습니다.

---

## 10. BFF 전환에 따른 클라이언트 사이드 코드 단순화 (Technical Debt 제거)

### 문제 상황

기존 JWT 방식에서는 401 에러 발생 시 토큰 갱신을 위해 `isRefreshing` 플래그, `refreshSubscribers` 큐, 복잡한 재시도 로직이 Axios 인터셉터에 포함되어 있어 유지보수가 어려웠습니다.

### 해결책

BFF 아키텍처 전환으로 브라우저가 직접 토큰을 다루지 않게 됨에 따라, 관련 로직을 모두 제거하고 **순수 세션 기반 인증**으로 단순화했습니다.

- **Axios 설정**: `withCredentials: true`를 기본값으로 설정하여 세션 쿠키 전송을 보장.
- **인터셉터 정리**: 복잡한 갱신 로직을 삭제하고, 401 발생 시 단순히 로그인 페이지로 리다이렉트하는 최소한의 로직만 남김.

### 결과

인증 관련 클라이언트 코드가 약 70% 감소하였으며, 로직이 단순해짐에 따라 잠재적인 레이스 컨디션 버그 가능성을 원천 차단했습니다.
