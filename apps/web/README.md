<div align="center">

# 🌐 전북대 수강신청 도우미 Frontend

**전북대 수강신청 도우미: Next.js 기반 실시간 여석 알림 및 스마트 커리큘럼 관리 플랫폼**

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-15.1.6-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/React-19.0-61DAFB?style=for-the-badge&logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?style=for-the-badge&logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-3.4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" />
  <img src="https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white" />
</p>

---

</div>

## 💎 핵심 가치 (Core Values)

- **⚡ Instant Response**: VAPID 기반 **Web Push**와 **Service Worker**를 활용하여 앱 종료 상태에서도 즉각적인 알림 전달
- **🎨 Premium UX**: **Bento Grid** 레이아웃과 **Framer Motion**을 활용한 현대적이고 미려한 인터페이스 제공
- **🧩 Smart Filtering**: 5,000개 이상의 강의 데이터를 조건별(이수구분, 학점, 시간대 등)로 즉시 필터링하는 고성능 검색 엔진
- **📱 True PWA**: 설치형 앱 지원과 오프라인 대응을 통해 모바일 앱과 동일한 사용자 경험 제공

---

## ️ 기술 스택 (Tech Stack)

### 🧱 Framework & UI

- **Next.js 15 (App Router)**, **React 19**
- **Tailwind CSS**, **shadcn/ui**, **Lucide Icons**
- **Framer Motion** (Interaction & Animation)

### 📡 Data Fetching & State

- **TanStack Query v5** (Server State Management, Infinite Scroll)
- **Zustand** (Client State Management, Auth Persistence)
- **Axios** (With Auto Token Refresh Interceptor)

### 🔔 Notification & PWA

- **Service Worker** & **Web Push API**
- **VAPID Authorization**
- **PWA (next-pwa)** (Standalone Mode Support)

---

## 📚 주요 기능 구현 (Key Features)

### 🏠 개인화 대시보드 (Bento Grid)

- **Bento Grid System**: 시간표, 공지사항, 최근 알림을 구획화하여 정보 가독성 극대화
- **실시간 위젯**: 총 신청 학점, 찜한 강의 수, 활성 알림 수를 한눈에 파악하는 통계 카드 제공

### 🔍 정밀 강의 검색 및 무한 스크롤

- **Infinite Scroll**: `useInfiniteQuery`와 `Intersection Observer`를 활용한 끊김 없는 데이터 브라우징

### 📝 강의 리뷰 및 커뮤니티

- **사용자 중심 리액션**: 내가 작성한 리뷰를 최상단에 고정하고, 타 리뷰에 실시간으로 공감/비공감 표현 가능
- **통계 가시화**: 강의별 평균 별점 및 리뷰 수를 상세 카드와 목록에 시각적으로 구현

### 📅 스마트 시간표 시뮬레이션

- **Interactive Grid**: 드래그 및 클릭 기반의 직관적인 시간표 편집 기능
- **검색 연동**: 검색 결과에서 즉시 시간표에 추가하고 중복 시간대를 체크하는 통합 UX

### 🚀 멀티 채널 알림 관리

- **Push Dedup**: 포그라운드(Toast)와 백그라운드(시스템 알림)를 구분한 하이브리드 알림 체계
- **Device Management**: 등록된 기기 목록 관리 및 원격 로그아웃, 알림 테스트 도구 제공

---

## 📂 프로젝트 구조 (Structure)

```text
src/
├── 📂 app            # Next.js App Router (Page & Layout)
├── 📂 features       # 도메인 기반 기능 모듈 (Auth, Course, Timetable 등)
│   ├── 📂 components # 기능별 특화 컴포넌트
│   ├── 📂 hooks      # React Query 및 커스텀 로직
│   └── 📂 store      # 도메인별 Zustand Store
├── 📂 shared         # 공통 컴포넌트, 유틸리티, 타입 정의
│   ├── 📂 ui         # shadcn/ui 기반 원자 컴포넌트
│   ├── 📂 api        # Axios 인스턴스 및 인터셉터
│   └── 📂 lib        # 공통 함수 및 포맷터
└── 📂 widgets        # 대시보드 등 페이지 구성을 위한 대형 UI 블록
```

---

## 🛠️ 트러블슈팅 및 성능 최적화

- **자동 토큰 갱신**: 401 에러 감지 시 인터셉터를 통해 토큰 리프레시 후 실패한 요청을 자동 재시도하는 로직 구현
- **인앱 브라우저 대응**: 카카오톡/에브리타임 등의 인앱 브라우저에서 Google 로그인 차단 문제를 감지하여 외부 브라우저 실행 가이드 제공
- **알림 시인성 개선**: `requireInteraction: true` 옵션과 인앱 커스텀 토스트를 연동하여 중요 알림 누락 방지

---

## 🔗 관련 문서 (Docs)

- 📜 **[릴리스 노트 (v1.1.0)](./docs/feature-updates.md)**
- 🛠️ **[트러블슈팅 로그](./docs/troubleshooting.md)**
