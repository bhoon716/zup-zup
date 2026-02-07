# JBNU Sugang Helper Web

Next.js 16 (App Router) 기반의 JBNU 수강신청 빈자리 알림 서비스 프론트엔드입니다.

---

## ✨ 주요 기능 (Key Features)

- **실시간 대시보드**: 구독 중인 강의 목록 및 여석 상태 실시간 확인.
- **강의 검색**: 학수번호, 과목명, 교수명은 물론 **학점, 시수(10+), 교양 영역/상세구분**까지 아우르는 강력한 필터링 제공.
- **조건부 필터링 UI**: 이수구분이 '교양'인 경우에만 상세 카테고리 필터가 노출되어 직관적인 설정 가능.
- **강의 상세**: **Chart.js**를 이용한 좌석 변동 이력을 시각화하여 경쟁률 추이 파악 가능.
- **프리미엄 대시보드 (Bento Grid)**: 정보를 구획화하여 시각적 직관성을 높인 현대적인 그리드 시스템 도입. 시각적 몰입감을 위해 대표 시간표를 전면에 배치하고 배경 데코레이션 및 애니메이션 강화.
- **비로그인 게스트 모드**: 로그인 없이도 강의 검색, 상세 정보 조회, 여석 이력 확인이 가능한 개방형 서비스 구조.
- **스마트 로그인 유도 (Modal)**: 비로그인 상태에서 찜하기/구독 등 개인화 기능 접근 시, 페이지 이동 없이 현재 화면에서 즉시 로그인 가능한 모달 제공.
- **DashboardStats 위젯**: 총 신청 학점, 찜 목록 수, 활성 알림 수 등을 한눈에 볼 수 있는 요약 카드 제공.
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

### 4. 401 에러 대응: 자동 토큰 리프레시

- **문제**: 세션 혹은 토큰 만료 시 모든 API 요청이 실패하고 사용자가 강제 로그아웃되는 불편함.
- **해결**: Axios 응답 인터셉터에 자동 리프레시 로직을 구현. 401 에러 감지 시 백그라운드에서 `/api/auth/refresh`를 호출하여 세션을 갱신하고, 실패했던 원래 요청을 자동으로 재검색 및 재시도하여 중단 없는 UX 제공.

### 5. 팝업 중복 트리거 방지 (Dialog Event Bubbling)

- **문제**: 강의 카드 내부에 위치한 다이얼로그의 배경(Overlay) 클릭 시, 부모 카드 컴포넌트로 이벤트가 전파(Bubbling)되어 팝업이 닫히자마자 다시 열리는 현상.
- **해결**: 다이얼로그를 카드 외부로 격리(Structural Isolation)하고, `stopPropagation`을 적용하여 이벤트 흐름을 제어함으로써 오작동 해결.

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
