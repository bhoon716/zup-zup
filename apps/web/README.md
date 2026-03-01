<div align="center">

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
```

---

## 🔗 관련 문서 (Docs)

- 📜 **[릴리스 노트](./docs/feature-updates.md)**
- 🛠️ **[트러블슈팅 로그](./docs/troubleshooting.md)**
