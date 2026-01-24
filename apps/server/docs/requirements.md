# 요구사항 정의서 (Requirements)

본 문서는 `jbnu-sugang-helper` 프로젝트의 개발을 위한 상세 요구사항을 정의합니다. 본 문서는 AI Agent 및 개발자가 시스템을 구현하는 데 필요한 기능적, 비기능적 요구사항과 아키텍처 가이드를 포함합니다.

## 1. 프로젝트 개요 (Overview)

- **프로젝트명**: JBNU 수강신청 알림 도우미 (Sugang Helper)
- **목적**: 전북대학교 수강신청 시스템의 강좌 현황을 주기적으로 모니터링하여, 빈자리(여석) 발생 시 사용자에게 즉시 알림을 제공하는 서비스입니다.
- **핵심 가치**: 사용자가 수강신청 페이지를 계속 새로고침하지 않아도, 원하는 강의의 여석 발생 시점에 즉시 알림을 받아 수강신청 성공 확률을 높입니다.

## 2. 기능 요구사항 (Functional Requirements)

### 2.1. 강의 모니터링 (Course Monitoring) - [Implemented]

- **REQ-MON-01**: 시스템은 설정된 주기(기본 5분)마다 대상 강의의 최신 정보를 수집(크롤링)해야 합니다.
  - **Implementation**: `JbnuCourseApiClient`를 통해 JBNU 수강신청 시스템의 XML API와 통신합니다. `Jsoup`을 사용하여 XML 응답을 파싱합니다.
  - **Optimization**: 불필요한 헤더(`Sec-Fetch-*`, `Cookie` 등)를 제거하고 필수 헤더(`Referer`, `Origin`, `X-Requested-With`)만 사용하여 요청 속도와 개인정보 보호를 강화했습니다.
- **REQ-MON-02**: 수집해야 할 데이터 항목은 다음과 같습니다.
  - 강좌명 (Name)
  - 과목코드 (Subject Code) [New] (e.g., "12345")
  - 분반 (Class Number) [New] (e.g., "01" - String type to preserve leading zeros)
  - 교수명 (Professor)
  - 수강 정원 (Capacity)
  - 현재 신청 인원 (Current Count)
  - **여석 (Available Seats)**: `Capacity - Current Count` (DB 내 지속 필드가 아닌 계산된 값 사용 가능)
  - **최종 크롤링 시각 (Last Crawled Time)**: `lastCrawledAt` (데이터 변동과 무관하게 갱신)
- **REQ-MON-03**: 크롤링 실패 시 재시도 로직이 포함되어야 하며, 지속적인 실패 시 관리자(로그)에게 알려야 합니다.
  - **Error Handling**: `CustomException` 및 `ErrorCode`를 사용하여 표준화된 에러 처리를 적용했습니다. (e.g., `CRAWLER_CONNECTION_ERROR`, `CRAWLER_PARSING_ERROR`)
- **REQ-MON-04 (Configurable API)**: 시스템은 실제 JBNU 서버 호스트가 아닌 테스트용 모킹 주소를 사용할 수 있어야 합니다. [New]
  - **Implementation**: `application.properties`의 `jbnu.api.url` 속성을 통해 호출 주소를 유연하게 변경할 수 있도록 구성했습니다.

### 2.2. 변동 감지 및 알림 (Detection & Notification) - [Implemented]

- **REQ-DET-01**: 이전 수집 데이터와 현재 수집 데이터를 비교하여 상태 변화를 감지해야 합니다.
  - **Implementation**: `CourseCrawlerService`가 크롤링 직후 기존 데이터와 비교하여 상태를 업데이트합니다.
- **REQ-DET-02 (핵심)**: 여석이 `0`에서 `1 이상`으로 변경되는 **"빈자리 발생"** 이벤트를 정확히 감지해야 합니다.
  - **Implementation**: 여석 발생 시 `SeatOpenedEvent`가 발행됩니다.
- **REQ-NOT-01**: 빈자리 발생 이벤트 감지 시, 해당 강좌를 구독한 모든 사용자에게 알림을 발송해야 합니다. - [Implemented]
- **REQ-NOT-02**: 지원하는 알림 채널은 다음과 같습니다. - [Implemented]
  - **App Push (FCM)**: 모바일 앱 사용자 대상
  - **Email**: 이메일 구독자 대상 (SMTP 활용)
  - **Web Push**: 브라우저 사용자 대상 (VAPID)

### 2.3. 중복 방지 및 최적화 (Deduplication & Optimization)

- **REQ-OPT-01**: 동일한 강좌의 동일한 변동 사항에 대해 짧은 시간 내에 중복 알림이 발송되지 않도록 **Debounce** 또는 **Dedup** 메커니즘을 적용해야 합니다. (Redis 활용 권장)
- **REQ-OPT-02**: 알림 발송은 비동기 큐(Queue/Worker) 구조로 처리하여 크롤링 프로세스의 지연을 방지해야 합니다.

### 2.4. 사용자 및 인증 (User & Authentication) - [Implemented]

- **REQ-AUT-01 (Social Login)**: 자체 회원가입 없이 **Google OAuth2**를 통한 소셜 로그인만 지원합니다. (SecurityConfig, CustomOAuth2UserService, User Entity)
- **REQ-AUT-02**: 로그인 성공 시 JWT 기반의 Access Token과 Refresh Token을 발급합니다.
  - **Success Handler**: `OAuth2SuccessHandler`에서 토큰 발급 및 리다이렉트 처리
- **REQ-AUT-03**: 사용자는 학수번호(CourseKey)를 통해 원하는 강좌를 검색하고 구독할 수 있어야 합니다.
- **REQ-AUT-04**: 사용자는 자신의 구독 목록을 조회하고, 더 이상 알림을 원치 않는 강좌를 구독 취소할 수 있어야 합니다.

### 2.5. 보안 및 토큰 관리 (Security & Token) - [Implemented]

- **REQ-SEC-01 (JWT Structure)**:
  - **Access Token**: 짧은 만료 시간 (30분). API 요청 시 `Authorization: Bearer {token}` 헤더로 전송.
  - **Refresh Token**: 긴 만료 시간 (2주). `HttpOnly`, `Secure` 쿠키(`refresh_token`)로 전송하여 XSS 공격 방지.
- **REQ-SEC-02 (Token Rotation)**: Refresh Token을 사용하여 Access Token을 재발급(Reissue) 받을 수 있어야 합니다. (`/api/auth/refresh`)
- **REQ-SEC-03 (Redis Storage)**: 발급된 Refresh Token은 **Redis**에 저장하여 관리합니다.
  - Key: `RT:{email}`
  - Value: `refreshToken` (Rotation 시 비교 검증)
- **REQ-SEC-04 (Logout)**: 로그아웃 시 Redis에서 해당 사용자의 Refresh Token을 삭제하고, Access Token은 남은 유효기간 동안 Blacklist 처리합니다(`BL:{accessToken}`).

### 2.6. API 문서화 (API Documentation) - [Implemented]

- **REQ-DOC-01**: 모든 외부 노출 API는 **Swagger(OpenAPI 3)**를 통해 자동 문서화되어야 합니다.
- **REQ-DOC-02**: 모든 Controller, Request DTO, Response DTO에는 `@Tag`, `@Operation`, `@Schema` 어노테이션을 사용하여 상세 설명을 포함해야 합니다.
- **REQ-DOC-03**: API 명세에는 실제 JBNU 데이터 포맷(예: 10자리 과목코드, '과목코드-분반' 형태의 키 등)을 반영한 **Example Value**가 반드시 포함되어야 합니다.
- **REQ-DOC-04**: Swagger UI는 `/swagger-ui/index.html` 경로를 통해 접근 가능해야 하며, 인터랙티브한 API 테스트 환경을 제공해야 합니다.

## 3. 비기능 요구사항 (Non-Functional Requirements)

### 3.1. 기술 스택 (Tech Stack)

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.x
- **Database**: MySQL 8.x
- **Cache/Message Broker**: Redis 6.x 이상
- **Build Tool**: Gradle

### 3.2. 성능 및 안정성 (Performance)

- **Concurrency**: 수백 개의 강좌를 동시에 모니터링할 수 있는 병렬 처리(Virtual Threads 또는 Async)를 고려해야 합니다.
- **Scalability**: 크롤러와 알림 워커는 독립적으로 확장 가능해야 합니다.
- **Resilience**: 대상 사이트(학교 서버)의 응답 지연이나 일시적 장애에 대해 시스템이 멈추지 않고 적절히 대기하거나 건너뛰어야 합니다.

## 4. 데이터 모델 (Data Model)

### 4.1. 주요 엔티티 (Entities)

| Entity                  | Description           | Key Fields                                                   |
| :---------------------- | :-------------------- | :----------------------------------------------------------- |
| **User**                | 알림 수신 사용자      | `id`, `email`, `name`, `role`                                |
| **Course**              | 모니터링 대상 강좌    | `courseKey` (PK), `name`, `professor`, `capacity`, `current` |
| **Subscription**        | 사용자-강좌 구독 관계 | `userId`, `courseKey`, `isActive`                            |
| **UserDevice**          | 사용자 기기 정보      | `id`, `userId`, `type`(FCM/WEB), `token`, `p256dh`, `auth`   |
| **NotificationHistory** | 알림 발송 이력        | `id`, `userId`, `courseKey`, `title`, `message`, `channel`   |

## 5. 아키텍처 (Architecture Flow)

1. **Scheduler** triggers **Crawler** (every 5 mins).
2. **Crawler** fetches page & parses data -> updates **Course** entity.
3. If `availableSeats` changes from 0 to 1+:
4. **Detector** publishes `SeatOpenedEvent`.
5. **Notifier Worker** consumes event.
6. Checks Redis for separate/recent alerts (Dedup).
7. Finds all **Subscriptions** for the `courseKey`.
8. **Batch fetches** User and UserDevice data to prevent N+1 queries.
9. Sends alerts via FCM/Email/WebPush using **NotificationTarget** abstraction.

## 6. 최근 개선 사항 (Recent Improvements)

### 6.1. 클린 코드 리팩토링 (Clean Code Refactoring)

- **CustomException 지침 준수**: 모든 비즈니스 로직에서 `CustomException`으로 예외를 일원화하여 Global Exception Handler에서 처리합니다.
  - `FCM_SEND_ERROR`, `WEBPUSH_SEND_ERROR` 등 ErrorCode 확장
  - Service/Entity 내부에서 `log.error`, `log.warn` 제거 (Global Handler에서 일괄 처리)
- **NotificationTarget DTO 도입**: 다양한 알림 채널의 수신자 정보를 통합 관리
  - Email, FCM 토큰, Web Push 키를 하나의 객체로 캡슐화
  - `NotificationSender` 인터페이스의 추상화 강화

### 6.2. 성능 최적화 (Performance Optimization)

- **N+1 쿼리 문제 해결**:
  - 기존: 구독자마다 `userRepository.findById()`, `userDeviceRepository.findByUserId()` 개별 호출
  - 개선: `userRepository.findAllById()`, `userDeviceRepository.findByUserIdIn()` 배치 조회
  - **결과**: DB 요청 횟수 대폭 감소, 알림 발송 성능 향상

- **JSON 직렬화 개선**:
  - Web Push 페이로드 생성 시 수동 문자열 조작 대신 `ObjectMapper` + `record` 사용
  - 안전하고 유지보수가 쉬운 방식으로 전환

### 6.3. 로컬 개발 환경 개선

- **H2 Database 지원**: 별도 MySQL 설치 없이 로컬 개발 가능
- **환경 변수 기본값**: 모든 필수 환경변수에 더미 기본값 추가
- **즉시 실행 가능**: `./gradlew bootRun` 명령어만으로 바로 실행

### 6.4. API 문서화 최적화 (API Documentation Optimization)

- **전수 Swagger 어노테이션 적용**:
  - 모든 컨트롤러 엔드포인트에 `@Operation` 및 `@ApiResponses` 적용
  - 모든 DTO 필드에 `@Schema`를 통한 설명 및 예시 데이터 추가
- **실제 데이터 기반 예시(Example) 정밀 교정**:
  - JBNU 실제 XML 응답 데이터를 분석하여 `courseKey`(`0000130844-1`), `subjectCode`(`0000130844`) 등 도메인 특화 포맷을 예시에 반영
  - `sentAt`, `createdAt` 등 시간 관련 필드에 표준 ISO 포맷 예시(`2024-01-01T12:00:00`) 보강
- **프론트엔드 협업 효율화**: 명확한 API 명세를 통해 클라이언트 팀의 개발 편의성 증대 및 커뮤니케이션 비용 감소

## 7. 테스트 및 검증 (Testing & Verification) - [New]

### 7.1. 비수강신청 기간 테스트 전략

실제 학교 서버의 데이터 변화가 없는 기간에도 시스템의 핵심 로직을 검증할 수 있어야 합니다.

- **REQ-TEST-01 (End-to-End Integration)**: 크롤링부터 알림 전송까지의 전체 흐름을 테스트 코드로 검증해야 합니다.
  - **Implementation**: `@SpringBootTest`와 Mock XML 데이터를 사용하여 실제와 동일한 시나리오를 시뮬레이션합니다.
- **REQ-TEST-02 (Vacancy Scenarios)**: 다음과 같은 잔여석 변화 케이스를 모두 검증해야 합니다.
  - `0 -> 0` (여전히 만석): 알림 발송 안 됨.
  - `0 -> 1+` (여석 발생): 알림 발송 **성공**.
  - `1+ -> 1+` (여석 유지): 알림 발송 안 됨.
- **REQ-TEST-03 (Sync Handling in Test)**: 테스트의 결정성을 위해 비동기(`@Async`) 로직을 동기적으로 처리할 수 있어야 합니다.
  - **Implementation**: `TestAsyncConfig`를 통해 `SyncTaskExecutor`를 주입하여 즉각적인 검증을 지원합니다.
