<div align="center">

# 🚀 전북대 수강신청 도우미 Backend

**전북대 수강신청 도우미: 실시간 여석 감지 및 멀티 채널 알림 서버**

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-7.0-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</p>

---

</div>

## 💎 핵심 가치 (Core Values)

- **🛡️ Reliability**: Redis 기반 중복 알림 방지 및 크롤러 중복 실행 방지 로직을 통한 데이터 무결성 확보
- **📡 Scalability**: 비동기 이벤트 기반 아키텍처를 통한 멀티 채널(FCM/Email/Discord) 확장성
- **⚡ Efficiency**: 학기 전환 대응 최적화 및 실시간 시스템 모니터링 관리 도구 제공

---

## 🏗️ 시스템 아키텍처 (Architecture)

```mermaid
graph TD
    subgraph "External Systems"
        OASIS["JBNU OASIS API"]
    end

    subgraph "Backend Core"
        Scheduler["Spring Scheduler"] --> Crawler["OASIS Crawler (Jsoup)"]
        Crawler --> Parser["XML/HTML Parser"]
        Parser --> DB[(MySQL)]
        DB --> Event["SeatOpenedEvent"]
        Event --> Notify["Notification Service"]
    end

    subgraph "Messaging Hub"
        Notify --> FCM["Firebase (FCM)"]
        Notify --> WebPush["Web Push (VAPID)"]
        Notify --> SMTP["Email (JavaMail)"]
        Notify --> Discord["Discord Bot"]
    end

    subgraph "Storage & Session"
        Notify --> Redis[(Redis Dedup)]
        Security["Spring Security"] --> Redis
    end
```

---

## �️ 기술 스택 (Tech Stack)

### 🧱 Framework & Language

- **Java 21 LTS**, **Spring Boot 3.5.x**
- **Spring Security** (OAuth2, JWT with Refresh Token Rotation)

### 💾 Data & Persistence

- **MySQL 8.0**, **Spring Data JPA**, **QueryDSL**
- **Redis** (Notification Dedup & Session Storage)
- **Flyway** (Database Version Control)

### 📢 Communication & Messaging

- **Firebase Admin SDK** (FCM)
- **WebPush (VAPID)**
- **JavaMail** (SMTP)
- **Discord Bot API**

---

## 📚 주요 기능 구현 (Key Implementation)

### 🔍 실시간 강의 수집 및 정밀 검색

- **Jsoup 크롤링**: `AtomicBoolean` 제어로 중복 실행을 원천 차단한 안정적인 수집 엔진
- **시간 범위 검색**: `dayOfWeek + startTime + endTime` 기반의 정밀 필터링 (QueryDSL 최적화)

### 🚀 비동기 멀티 채널 알림

- **이벤트 기반 아키텍처**: `SeatOpenedEvent` 발행을 통한 수집과 알림 로직의 완전 분리
- **Redis Dedup**: 10분 이내 동일 과목에 대한 중복 알림을 방지하여 피로도 최소화

### 🛡️ 관리자 관제 센터

- **실시간 모니터링**: 대시보드 통계(가입자, 알림 발송) 및 실시간 활동 로그 추적
- **운영 유연성**: GUI를 통한 크롤링 타겟(년도/학기) 및 학부 일정 즉시 제어

---

## 📂 프로젝트 구조 (Structure)

```text
src/main/java/bhoon/sugang_helper
├── ⚙️ common             # 전역 보안, 응답 규약, 예외 처리
└── 📦 domain
    ├── 👤 auth / user    # 인증 및 사용자 관리
    ├── 📖 course         # 강의 정보 수집 및 검색
    ├── 🔔 notification   # 멀티 채널 알림 발송
    ├── 📅 schedule       # 학사 일정 관리
    ├── 📝 announcement   # 마크다운 공지사항
    ├── 🕒 timetable      # 유저 시간표 관리
    └── 🛠️ admin          # 시스템 통합 관제
```

---

## � 관련 문서 (Docs)

- 📜 **[릴리스 노트 (v1.0.0)](./docs/feature-updates.md)**
- 🛠️ **[트러블슈팅 로그](./docs/troubleshooting.md)**
