# JBNU Sugang Helper Web

Next.js 16 (App Router) 기반의 JBNU 수강신청 빈자리 알림 서비스 프론트엔드입니다.

---

## ✨ 주요 기능 (Key Features)

- **실시간 대시보드**: 구독 중인 강좌 목록 및 여석 상태 실시간 확인.
- **강좌 검색**: 학수번호, 과목명, 교수명은 물론 **학점, 시수(10+), 교양 영역/상세구분**까지 아우르는 강력한 필터링 제공.
- **조건부 필터링 UI**: 이수구분이 '교양'인 경우에만 상세 카테고리 필터가 노출되어 직관적인 설정 가능.
- **강좌 상세**: **Chart.js**를 이용한 좌석 변동 이력을 시각화하여 경쟁률 추이 파악 가능.
- **예비 수강 바구니 (Wishlist)**: 관심 있는 강좌를 찜해두고 한눈에 모아볼 수 있는 위시리스트 기능.
- **Smart Web Push**: VAPID 기반 웹푸시를 지원하며, **포그라운드 상태에서도 인앱 Toast 알림**을 통해 놓치지 않는 알림 경험 제공.
- **알림 테스트 도구**: 관리자 전용 페이지에서 이메일/웹푸시/앱푸시 알림을 즉시 테스트하고 기기를 원클릭으로 등록 가능.
- **UX 최적화**: 검색 실행 시 필터바 **자동 접힘(Auto-collapse)** 및 스켈레톤 UI 적용.
- **반응형 디자인**: 모바일 및 태블릿 환경에 최적화된 유연한 레이아웃.

---

## 🛠 기술 스택 (Tech Stack)

- **Core**: Next.js 16, React 19, TypeScript
- **Styling**: Tailwind CSS, shadcn/ui (Radix UI)
- **Notifications**: Service Worker, Web Push API, Sonner (Toast)
- **Data Fetching**: TanStack Query v5 (React Query)
- **State Management**: Zustand (인증 정보 관리)
- **Testing**: Vitest, React Testing Library, MSW
- **Build**: Turbopack

---

## 🚀 트러블슈팅 (Troubleshooting)

상세 내용은 [Web Troubleshooting Log](docs/troubleshooting.md)에서 확인할 수 있습니다.

### 1. Web Push 무한 로딩 및 타임아웃 처리

- **문제**: 브라우저 환경에 따라 서비스 워커(`navigator.serviceWorker.ready`) 응답이 지연되어 기기 등록 시 무한 로딩 발생.
- **해결**: `Promise.race`를 활용하여 5초 타임아웃 로직을 추가하고, 실패 시 사용자에게 명확한 에러 피드백을 제공하여 UX 개선.

### 2. 포그라운드 알림 시인성 문제 (이중 알림 구조)

- **문제**: 브라우저 탭이 활성화(Foreground)된 상태에서는 OS 정책상 푸시 알림이 뜨지 않거나 금방 사라져 사용자가 인지하기 어려움.
- **해결**: 서비스 워커에서 푸시 수신 시 `postMessage`로 클라이언트(탭)에 신호를 보내고, 클라이언트는 이를 감지하여 **인앱 Toast(sonner)**를 띄우는 이중 알림 구조 구현. 또한 `requireInteraction: true` 옵션을 통해 사용자가 확인하기 전까지 알림 유지.

### 3. VAPID 키 환경 변수 연동

- **문제**: `NEXT_PUBLIC_` 접두사가 없는 환경 변수를 클라이언트 사이드 코드에서 참조하여 키 값이 `undefined`로 나오는 문제.
- **해결**: `.env` 변수명을 수정하고 유틸리티 함수에서 키 존재 여부를 엄격하게 체크하도록 에러 핸들링 강화.

### 4. API 요청 중복 제거 (Request Deduplication)

- **문제**: Zustand와 React Query의 독립적인 호출로 인해 `/me` API가 중복 발생하는 성능 낭지.
- **해결**: API 모듈에서 **Promise Caching**을 도입하여 진행 중인 요청을 공유함으로써 네트워크 비용 절감 및 초기 로딩 성능 개선.

---

## 🔧 설치 및 실행 (Setup)

### 1. 환경 변수 설정

`web/.env` 파일을 생성하고 다음 값을 설정합니다.

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_VAPID_KEY=your_vapid_public_key_from_server
```

### 2. 의존성 설치 및 실행

```bash
cd web
npm install
npm run dev
```

---

## 🧪 테스팅 (Testing)

모든 핵심 유틸리티 및 React Query Hook에 대한 단위 테스트가 포함되어 있습니다.

```bash
# 단위 테스트 실행 (Vitest)
npm run test
```

---

## 📦 폴더 구조 (Project Structure)

```text
src/
├── app/            # Next.js App Router
├── components/     # UI 및 도메인 컴포넌트
│   ├── features/   # 기능별 컴포넌트 (알림, 구독 등)
│   └── providers.tsx # 전역 프로바이더 (QueryClient, Auth, Toast)
├── hooks/          # 커스텀 Hook (기기 등록, 알림 테스트 등)
├── lib/            # API 클라이언트 및 Web Push 유틸
├── store/          # Zustand 스토어
└── public/         # 정적 파일 (sw.js 포함)
```
