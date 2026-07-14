# 줍줍 Web

<p align="center">
  사용자가 가장 먼저 마주하는 화면. 검색, 시간표, 리뷰, 알림을 담당하는 프론트엔드.
</p>

줍줍 Web은 사용자가 수강신청 판단을 시작하는 첫 지점입니다.
빠르게 읽히는 화면, 덜 끊기는 탐색 흐름, 명확한 알림 표현을 중심으로 설계했습니다.

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 역할 | 사용자 화면, 검색, 시간표, 리뷰, 알림 UI |
| 성격 | Next.js 기반 프론트엔드 |
| 배포 | Vercel |
| 로그 | `/var/log/jbnu-sugang-helper/web/web.log` |

## 왜 이 앱이 필요한가

- 수강신청은 정보가 빠르게 바뀌기 때문에, 사용자가 즉시 판단할 수 있는 화면이 필요합니다.
- 검색, 시간표, 리뷰, 알림이 분리되면 맥락을 다시 읽는 비용이 커집니다.
- 프론트엔드는 기능 목록보다도 흐름의 단순성이 더 중요합니다.

## 핵심 책임

- 강의 검색과 필터 조작
- 시간표 기반 탐색 경험
- 리뷰와 알림 관련 화면
- 서버 응답을 읽기 쉬운 형태로 재구성하는 표시 계층

## 설계 포인트

| 선택 | 이유 | 효과 |
| --- | --- | --- |
| TanStack Query | 서버 상태를 명확하게 관리하기 위해 | 검색, 갱신, 재요청 흐름이 안정적입니다 |
| Zustand | 가벼운 UI 상태를 분리하기 위해 | 전역 상태가 과도하게 커지지 않습니다 |
| React Hook Form + Zod | 폼과 검증을 함께 정리하기 위해 | 입력과 검증 로직이 단순해집니다 |
| Radix UI + Tailwind | 접근성과 일관된 스타일을 동시에 가져가기 위해 | 구현 속도와 UI 품질을 함께 확보합니다 |
| Framer Motion | 화면 전환과 강조를 부드럽게 전달하기 위해 | 정보의 우선순위가 더 잘 보입니다 |

## UI 관점에서 본 역할

- 검색 결과는 빠르게 읽히는 구조를 우선합니다.
- 시간표와 목록은 판단에 필요한 정보를 한 번에 보여주는 방향으로 설계합니다.
- 알림과 리뷰 흐름은 오래 머무르지 않아도 이해되도록 단순하게 유지합니다.

## 기술 스택

- Next.js 16.3.0-preview.5
- React 19.2.7
- TanStack Query
- Zustand
- React Hook Form
- Zod
- Radix UI
- Tailwind CSS
- Sonner
- Framer Motion

## Local Run

```bash
cd ../..
npm ci
npm run web:dev
```

## Common Commands

- `npm run build`
- `npm run start`
- `npm run e2e`
- `npm run lint`
- `npm run test`

## Related Docs

- [Project release notes](../../docs/feature-updates.md)
- [Troubleshooting log](../../docs/troubleshooting.md)

## Operational Notes

- 배포 기준은 루트 README를 참고합니다.
- Node.js 22.x와 루트 `package-lock.json`이 배포·CI의 기준입니다.
- Web 로그는 `/var/log/jbnu-sugang-helper/web/web.log`에 남습니다.
- 이 문서는 사용자 화면과 정보 구조를 이해하는 데 초점을 둡니다.
