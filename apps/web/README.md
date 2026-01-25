# JBNU Sugang Helper Web

Next.js 16 (App Router) 기반의 JBNU 수강신청 빈자리 알림 서비스 프론트엔드입니다.

---

## ✨ 주요 기능 (Key Features)

- **실시간 대시보드**: 구독 중인 강좌 목록 및 여석 상태 확인.
- **강좌 검색**: 학수번호, 과목명, 교수명 기반의 강력한 검색 기능.
- **강좌 상세**: **Chart.js**를 이용한 시간별 인원 변동 추이 시각화.
- **알림 내역**: 수신된 알림의 상세 정보 및 타임라인 확인.
- **알림 설정**: Web Push 기기 등록 및 알림 수신 방법 제어.
- **계정 관리**: 프로필 수정 및 **AlertDialog**를 통한 안전한 회원 탈퇴 절차.
- **UI 구조**: **Tabs**를 활용한 체계적인 설정 페이지 구성.
- **관리자 모드**: 서비스 전체 통계 및 운영 현황 대시보드 (RBAC 적용).

---

## 🛠 기술 스택 (Tech Stack)

- **Core**: Next.js 16, React 19, TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: shadcn/ui (Radix UI), Lucide React
- **Data Fetching**: TanStack Query v5 (React Query)
- **State Management**: Zustand (인증 정보 관리)
- **Testing**: Vitest, React Testing Library, MSW (Mock Service Worker)
- **Build**: Turbopack

---

## 🔧 설치 및 실행 (Setup)

### 1. 환경 변수 설정

`web/.env.local` 파일을 생성하고 다음 값을 설정합니다.

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_VAPID_PUBLIC_KEY=your_vapid_public_key
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

# 테스트 리포트 확인
npm run test:ui (선택 사항)
```

---

## 📦 폴더 구조 (Project Structure)

```text
src/
├── app/            # Next.js App Router (Page & Layout)
├── components/     # UI 및 도메인 단위 컴포넌트
│   ├── ui/         # 추상화된 UI Base (Button, Card 등)
│   ├── features/   # 기능별 컴포넌트 (Search, Settings 등)
│   └── layout/     # 페이지 공통 레이아웃
├── hooks/          # React Query 및 커스텀 Hook
├── lib/            # API 클라이언트 및 유틸리티
├── store/          # Zustand 스토어
├── types/          # TypeScript 인터페이스/타입 정의
└── test/           # Vitest 테스트 설정 및 유틸
```
