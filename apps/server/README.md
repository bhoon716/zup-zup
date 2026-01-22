# JBNU 수강신청 알림 도우미 (Sugang Helper)

전북대학교 수강신청 현황을 주기적으로 크롤링하여 **빈자리(여석)**가 생기면 즉시 알림을 보내주는 서비스입니다.

## ✨ 주요 기능

- **강의 모니터링**: 5분 주기로 수강신청(또는 강의 조회) 페이지를 크롤링하여 정원 및 여석 정보를 수집합니다.
- **빈자리 알림**: 여석이 `0`에서 `1` 이상으로 변경되는 순간을 감지하여 알림을 발송합니다.
- **중복 방지 (Dedup)**: Redis를 활용하여 동일한 변동 사항에 대해 중복 알림이 가지 않도록 제어합니다.
- **다양한 알림 채널**:
  - 앱 푸시 (FCM)
  - 이메일
  - 웹 푸시 (WebPush)
- **구독 관리**: 사용자가 원하는 강의만 선택하여 알림을 받을 수 있습니다.
- **간편 로그인**: **Google OAuth2** 로그인을 지원합니다.

## 🛠 기술 스택 (Tech Stack)

| Category         | Technology         | Note                            |
| :--------------- | :----------------- | :------------------------------ |
| **Backend**      | Spring Boot        | 버전 미정                       |
| **Database**     | MySQL              | 8.x 권장                        |
| **Cache/Queue**  | Redis              | Token 관리, Dedup               |
| **Notification** | FCM, SMTP, WebPush | VAPID 지원 예정                 |
| **Auth**         | Google OAuth2, JWT | Access(Header), Refresh(Cookie) |

## 🏗 아키텍처 (Architecture)

1.  **Scheduler**: 설정된 주기(예: 5분)마다 크롤링 작업을 트리거합니다.
2.  **Crawler**: 대상 페이지를 호출하여 정원, 신청 인원, 여석 데이터를 파싱합니다.
3.  **Detector**: 수집된 데이터를 이전 데이터와 비교하여 **여석 발생(0 → 1+)** 이벤트를 감지합니다.
4.  **Notifier Worker**: 감지된 이벤트를 큐에서 가져와 각 채널(앱, 이메일 등)로 알림을 발송합니다.
5.  **Dedup/Rate Limit**: Redis를 이용해 중복 발송을 막고 사용자/강의별 과다 발송을 방지합니다.

## 💾 데이터 모델 (Data Model)

- **User**: `email`, `name`, `provider`, `providerId`, `role`
- **Course**: `courseKey`, `name`, `professor`, `capacity`, `availableSeats`, `lastCrawledAt`
- **Subscription**: `userId`, `courseKey`, `active/paused`
- **NotificationLog**: `courseKey`, `seatChange`, `result`, `retries`

## 🚀 시작하기 (Getting Started)

### 사전 요구사항 (Prerequisites)

프로젝트를 실행하기 위해 다음 환경이 필요합니다.

- JDK 21
- MySQL 8.x
- Redis 6.x+

### 환경 변수 (Environment Variables)

실행 전 `application.yml` 또는 환경 변수로 아래 설정을 완료해야 합니다.

| Variable                 | Description         | Example       |
| :----------------------- | :------------------ | :------------ |
| `SPRING_PROFILES_ACTIVE` | 실행 프로파일       | `local`       |
| `DB_HOST`                | MySQL 호스트        | `localhost`   |
| `DB_PORT`                | MySQL 포트          | `3306`        |
| `DB_NAME`                | 데이터베이스명      | `jbnu_helper` |
| `DB_USER`                | 데이터베이스 사용자 | `root`        |
| `DB_PASSWORD`            | 데이터베이스 암호   | `password`    |
| `REDIS_HOST`             | Redis 호스트        | `localhost`   |
| `REDIS_PORT`             | Redis 포트          | `6379`        |

### API (Draft)

API 명세는 개발 진행에 따라 변경될 수 있습니다.

| Method   | Endpoint                       | Description      |
| :------- | :----------------------------- | :--------------- |
| `GET`    | `/oauth2/authorization/google` | 구글 로그인 시작 |
| `POST`   | `/api/auth/refresh`            | 토큰 재발급      |
| `GET`    | `/api/courses`                 | 강의 검색/조회   |
| `POST`   | `/api/subscriptions`           | 강의 구독        |
| `GET`    | `/api/subscriptions`           | 내 구독 목록     |
| `DELETE` | `/api/subscriptions/{id}`      | 구독 취소        |

## 🗺 로드맵 (Roadmap)

- [ ] **Phase 1**: 크롤러 PoC (강의 파싱 및 DB 저장)
- [ ] **Phase 2**: 스케줄러 적용 및 여석 변동 감지(Diff)
- [ ] **Phase 3**: Redis 기반 중복 방지 및 알림 큐 구축
- [ ] **Phase 4**: 이메일 알림 연동
- [ ] **Phase 5**: 앱 푸시(FCM) 및 웹 푸시 연동
