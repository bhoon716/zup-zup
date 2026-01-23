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
| **Backend**      | Spring Boot        | 3.4.x (Java 21)                 |
| **Database**     | MySQL              | 8.x 권장                        |
| **Cache/Queue**  | Redis              | Token 관리(RT/Blacklist), Dedup |
| **Notification** | FCM, SMTP, WebPush | VAPID 지원 예정                 |
| **Auth**         | Google OAuth2, JWT | Access(Header), Refresh(Cookie) |
| **Crawler**      | Jsoup              | XML API 통신, 헤더 최적화       |

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

프로젝트 루트에 `.env` 파일을 생성하여 관리합니다. (예제: `.env.example` 참고)
`application.properties`는 이 환경변수를 참조하도록 설정되어 있습니다.

```properties
# Database
DB_URL=jdbc:mysql://localhost:3306/sugang_helper?useSSL=false
DB_USERNAME=root
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Google OAuth2
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret

# JWT
JWT_SECRET=your_jwt_secret_key_should_be_long_enough

# CORS & Redirect
ALLOWED_ORIGINS=http://localhost:3000
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect
```

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

- [x] **Phase 0**: 프로젝트 초기 세팅 및 인증 시스템 구현 (OAuth2, JWT, Redis)
- [x] **Phase 1**: 크롤러 PoC (강의 파싱 및 DB 저장, 헤더 최적화 완료)
- [ ] **Phase 2**: 스케줄러 적용 및 여석 변동 감지(Diff)
- [ ] **Phase 3**: Redis 기반 중복 방지 및 알림 큐 구축
- [ ] **Phase 4**: 이메일 알림 연동
- [ ] **Phase 5**: 앱 푸시(FCM) 및 웹 푸시 연동
