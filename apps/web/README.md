<div align="center">
<<<<<<< Updated upstream

# 🎨 전북대 수강신청 도우미 Front-End

**전북대 수강신청 도우미: 고도화된 UX와 시인성 중심의 프론트엔드 플랫폼**

<p align="center">
  <img src="https://img.shields.io/badge/Next.js-15.1-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/React-19.0-61DAFB?style=for-the-badge&logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwindcss&logoColor=white" />
  <img src="https://img.shields.io/badge/TanStack_Query-FF4154?style=for-the-badge&logo=react-query&logoColor=white" />
</p>

---

</div>

## ✨ 핵심 가치 (Core Values)

- **⚡ Speed**: TanStack Query를 활용한 지연 없는 강의 데이터 탐색
- **🎨 Intuition**: 드래그 한 번으로 시간을 고르는 스마트 그리드 필터
- **🛡️ Reliability**: 브라우저 상태를 자동 감지하는 Self-Healing 웹푸시

---

## 🛠️ 기술 스택 (Tech Stack)

### 🧱 Framework & Language

- **Next.js 15+ (App Router)**, **React 19**, **TypeScript**
- **Axios** (Security Interceptors with Refresh Token Rotation)

### 🎨 Styling & Animation

- **Tailwind CSS**, **shadcn/ui**, **Framer Motion**
- **Lucide Icons** (Visual Semantics)

### 📡 Data & Workflow

- **TanStack Query v5** (Caching & State Consistency)
- **Firebase Web SDK** (Web Push & Message Handler)
- **React Markdown** (High-Quality Docs Rendering)

---

## 🏗️ 사용자 경험 혁신 (UX Innovation)

### 📅 가상 시간표 시뮬레이터

- **Why**: 여러 강의를 담았을 때 시간이 겹치는지, 점심시간은 확보되는지를 즉각적으로 확인해야 함.
- **What**: 겹치는 강의를 세로 분할(`column-split`)하여 렌더링하는 전용 시간표 그리드 로직 구현.
- **Impact**: 수강 바구니 구성의 불확실성을 제거하고 모바일에서도 안정적인 가독성 확보.

### 🔔 Self-Healing 알림 시스템

- **Why**: 브라우저 정책 변화나 만료된 구독 정보로 인한 알림 누락은 치명적인 정보 손실임.
- **What**: 서비스 워커 등록 상태를 감시하고, 구독 불일치 시 자동으로 `Unsubscribe - Resubscribe`를 수행.
- **Impact**: 푸시 알림 신뢰도를 95% 이상으로 유지하며 사용자 경험 보장.

### 📝 마크다운 기반 운영 소통

- **Why**: 복잡한 공문을 깔끔하게 보고 운영자가 자유롭게 정보를 전달할 공간이 필요함.
- **What**: `MarkdownViewer` 공통 컴포넌트 구축 및 관리자용 4:6 분할 에디터 제공.
- **Impact**: 정보 전달의 정확성을 높이고 운영 생산성 극대화.

---

## 📂 프로젝트 구조 (Feature-Based)

```text
src
├── app             # 라우트 설계 및 페이지 레이아웃
├── features        # 도메인 기반 기능 모듈
│   ├── user / auth  # 회원 정보 및 인증 흐름
│   ├── course       # 강의 엔진 및 상세 필터
│   ├── timetable    # 그리드 시뮬레이터 로직
│   ├── notification # 웹푸시/디바이스 관리
│   └── admin        # 운영 관제 시스템
└── shared           # 공용 UI 컴포넌트, 유틸리티, 타입
=======
  
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

> [!IMPORTANT]
> **Legal Disclaimer & Non-Profit Notice**
> 본 서비스는 전북대학교 학생들을 위한 **공익적 목적의 비영리 서비스**입니다. 제공되는 정보는 보조적인 도구이며, 실제 결과에 대한 법적 책임은 사용자 본인에게 있습니다.

---

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

- **고도화된 필터링**: 학수번호, 교수명은 물론 '교양 상세 구분' 등 계층형 필터링 UI 구현
- **Infinite Scroll**: `useInfiniteQuery`와 `Intersection Observer`를 활용한 끊김 없는 데이터 브라우징

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
>>>>>>> Stashed changes
```

---

<<<<<<< Updated upstream
## 🔗 관련 문서 (Docs)

- 📜 **[릴리스 노트](./docs/feature-updates.md)**
- 🛠️ **[트러블슈팅 로그](./docs/troubleshooting.md)**
=======
## 🛠️ 트러블슈팅 및 성능 최적화

- **자동 토큰 갱신**: 401 에러 감지 시 인터셉터를 통해 토큰 리프레시 후 실패한 요청을 자동 재시도하는 로직 구현
- **인앱 브라우저 대응**: 카카오톡/에브리타임 등의 인앱 브라우저에서 Google 로그인 차단 문제를 감지하여 외부 브라우저 실행 가이드 제공
- **알림 시인성 개선**: `requireInteraction: true` 옵션과 인앱 커스텀 토스트를 연동하여 중요 알림 누락 방지

---

## 📜 관련 문서 (Docs)

- 🛠️ **[Web Troubleshooting Log](./docs/troubleshooting.md)**
>>>>>>> Stashed changes
