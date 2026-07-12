# 줍줍 Server

<p>
  서비스의 중심 로직을 담당하는 백엔드. 수집, 인증, 알림, 운영 API를 맡는다.
</p>

줍줍 Server는 데이터 흐름과 운영 안정성을 책임집니다.
프론트엔드가 읽기 쉬운 응답을 제공하고, 수강신청 관련 정보를 안정적으로 수집하고 전달하는 것을 목표로 합니다.

## 프로젝트 개요

| 항목 | 내용                                                   |
|----|------------------------------------------------------|
| 역할 | 강의 수집, 인증, 알림, 리뷰, 운영 API                            |
| 성격 | Spring Boot 기반 백엔드                                   |
| 운영 | OCI CPU + Docker                                     |
| 로그 | `/var/log/jbnu-sugang-helper/server/application.log` |

## 왜 이 앱이 필요한가

- 수강신청 관련 데이터는 수집과 가공, 인증, 알림까지 하나로 이어집니다.
- 프론트엔드에서 모든 책임을 처리하면 유지보수가 어렵습니다.
- 백엔드는 데이터 모델과 API 계약을 중심으로 안정성을 확보해야 합니다.

## 핵심 책임

- 강의 및 학사 데이터 수집
- 인증과 사용자 요청 처리
- 알림, 리뷰, 운영 API 제공
- 프론트엔드가 의존하는 응답 구조 유지

## 설계 포인트

| 선택                                    | 이유                                 | 효과                        |
|---------------------------------------|------------------------------------|---------------------------|
| Spring Boot                           | 웹, 보안, 검증, 배치 기능을 한 프레임워크에서 다루기 위해 | 서비스 책임을 한 곳에서 정리할 수 있습니다  |
| JPA                                   | 도메인 중심 데이터 접근을 위해                  | 쿼리보다 모델에 집중할 수 있습니다       |
| Security + OAuth2 Client + JWT        | 인증과 보호가 필요한 영역을 분리하기 위해            | 로그인과 권한 처리 흐름을 일관되게 유지합니다 |
| Redis                                 | 세션과 캐시, 상태성 데이터를 효율적으로 다루기 위해      | 응답 속도와 운영 유연성을 얻습니다       |
| Flyway                                | DB 변경 이력을 명확히 남기기 위해               | 스키마 변경을 추적하기 쉬워집니다        |
| Prometheus + Swagger + Testcontainers | 관측, 계약 확인, 통합 검증을 동시에 챙기기 위해       | 운영과 테스트 품질을 함께 올릴 수 있습니다  |

## 운영 관점

- 수집, 인증, 운영 기능이 서로의 책임을 넘지 않도록 분리합니다.
- 운영 환경에서는 로그와 배치성 작업을 중심으로 상태를 추적합니다.
- 알림, 이메일, 웹 푸시, 파일 검증 같은 외부 연동은 별도 라이브러리와 계층으로 나눕니다.

## 기술 스택

- Spring Boot 3.5.9
- Spring Data JPA
- Spring Security
- Spring Data Redis
- Flyway
- Spring Batch
- Swagger/OpenAPI
- Prometheus
- MySQL, H2
- Redis
- Firebase Admin
- Web Push
- Apache Tika

## Local Run

```bash
cd apps/server
./gradlew bootRun
```

## Common Commands

- `./gradlew test`
- `./gradlew migrationTest` (Docker가 필요한 Flyway fresh/upgrade 검증)
- `./gradlew manualTest`
- `./gradlew performanceTest`
- `./gradlew bootJar`

## Related Docs

- [Project release notes](../../docs/feature-updates.md)
- [Troubleshooting log](../../docs/troubleshooting.md)

## Operational Notes

- 운영 기준은 루트 README와 함께 보는 것이 좋습니다.
- Server 로그는 `/var/log/jbnu-sugang-helper/server/application.log`에 남습니다.
- 이 문서는 데이터 흐름과 안정성 관점에서 읽는 것이 가장 유용합니다.
