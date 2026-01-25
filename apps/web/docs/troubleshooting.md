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

검색 결과 및 구독 목록에서 강좌명, 교수명, 수강 인원 현황이 정확하게 노출됩니다.

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
