# 2026-10-26 단순화 브랜치 배포 점검

## 승인과 대상

사용자는 로컬 동작 확인 후 기존 정상 절차로 배포하도록 승인했다. 이번 배포에 필요한 정상 push·PR·필수 CI·병합은 허용하며 force push·보호 규칙 우회·새 서비스 비용·운영 데이터 삭제·자격 증명/보안 네트워크 설정 확대는 하지 않는다.

- 후보 브랜치: `codex/simplify-existing-20261026`.
- 기존 검증 코드: `93c750adec3b128f91b85b2f1fa0d1d26756624b`.
- 원격 main 통합 커밋: `83e3117a2eae0e2e60a04a7c5e7d2ec8993bd3a6`. 계절학기 수정은 이미 포함되어 있었으며 패키지·주석 충돌 해결 후 코드 트리는 검증본과 동일하다.
- PR: https://github.com/bhoon716/zup-zup/pull/35. main 필수 `CI`, 관리자 포함 보호 규칙과 정상 PR 경로를 따른다.
- 기존 운영 경로: 웹은 기존 Vercel 프로젝트, 서버는 `Production CD`의 OCI ARM64/Docker Compose. 공개 사이트는 https://www.zup-zup.com 이다.
- 사전 확인된 이전 main·성공한 OCI/Vercel Production SHA: `8efb5c4e544a9cd861fb6cbfec2b6cd229c05c7a`.

## 새 로컬 실행 점검

기존 테스트 결과만으로 실제 점검을 대신하지 않았다. 현재 전체 Spring 애플리케이션을 별도 프로세스로 실행하고 격리 MySQL 8.4·Redis 6에서 실제 JPA/트랜잭션/세션/HTTP 경로를 점검했다. 외부 OAuth authorization/token/userinfo는 localhost 가짜 제공자, 수집 응답과 알림 발송도 가짜 경계를 사용했다. 제품 코드에 로그인 우회나 테스트 엔드포인트를 추가하지 않았다.

새 HTTP 실행 점검 40개가 통과했다.

- 비로그인 401, 가짜 OAuth 로그인 교환 3계정, 로그인 세션 ID 회전, Max-Age/Expires 없는 HttpOnly/SameSite 브라우저 세션 쿠키.
- 로그인 사용자 DTO와 일반 인증 응답의 불필요 Set-Cookie 없음, 관리자 권한 거절, CSRF 누락 403/A003.
- 실제 DB 기반 강의 검색과 서버 페이지 크기 30, 구독 등록·중복 거절·취소.
- 가짜 수집 응답을 실제 파서→강의 저장→좌석 변경 감지→outbox 저장→순차 선점/가짜 발송→SENT·이력 정산으로 처리. 가짜 발송 1건, 두 번째 poll에서 재발송 없음.
- 관리자 일정 생성·일반 공개 조회·D-day 선택 조회·수정·soft delete·복원, 00:17~23:59 시각 보존.
- 실제 multipart PNG 문의 첨부 저장/검증, 소유자 다운로드, 다른 사용자 거절, 관리자 확인·CSRF 요구 후 다운로드.
- 현재 기기 로그아웃 후 보호 API 거절, 별도 관리자 세션 유지, 실제 DB Flyway V30 확인.

초기 점검 환경의 readiness 그룹 누락으로 로그인 후 도착지 /health가 503을 반환했다. 운영 템플릿과 같은 readinessState/db/redis 그룹을 임시 환경에 명시한 뒤 전체 점검을 다시 통과했다. 이를 OAuth 제품 결함으로 기록하거나 실패를 skip하지 않았다.

기존 회귀 결과도 보존했다: 서버 548·MySQL migration 2·Redis HTTP 7·웹 316·Playwright 9건, 정적 검사/타입/lint/서버·웹 빌드 통과. 실제 Google/Discord 교환, 운영 사용자 세션, 실제 Slack/메일/푸시 발송, 운영 부하 성능을 점검했다고 주장하지 않는다.

## DB와 사용자 전환

원격 main 대비 새 migration은 두 개뿐이다. 기존 migration 파일은 변경하지 않았다.

- V29: 일정 제목 길이 확장과 `dday_enabled=false`, `deleted_at` 추가. 기존 일정·D-day 테이블/행/ID를 삭제하지 않는다. 옛 D-day ID 호환/이관 계층은 추가하지 않으며 통합 일정에서 관리자가 표시 대상을 선택한다.
- V30: 명시적 성능 인덱스 22개 제거. PK·유니크·FK 필수·outbox 정합성·Redis 세션 인덱스는 보존한다. [목록과 복구 SQL](refactoring/performance-index-removal.md).

MySQL DDL은 여러 테이블 전체의 원자적 rollback을 보장하지 않는다. 인덱스 제거로 검색·집계·이력·발송 선점의 지연과 잠금 대기가 늘 수 있다. 격리 DB의 기능 검증은 운영 처리량/잠금 보장이 아니다. 기존 스키마가 migration과 다르면 Flyway 실패 원인을 확인하며 blind repair/clean을 하지 않는다.

**모든 사용자는 전환 후 한 번 재로그인이 필요하다.** 새로운 Redis 세션 namespace는 옛 자체 JWT·refresh·기존 세션을 인증에 사용하지 않는다. 예전 Redis 키·운영 사용자 데이터는 삭제하지 않는다. 웹·서버의 정상 응답과 CSRF 계약이 함께 바뀌므로 두 배포가 모두 준비되기 전 일시적으로 로그인이 필요하거나 기능이 사용 불가할 수 있다. 기존 일시 중단 허용 정책을 따른다.

## 정상 배포와 복귀 경계

1. 추가 로컬 실행 결과와 CI를 확인한 뒤 draft를 해제하고, 필수 CI가 성공한 정확한 head SHA로 PR을 정상 병합한다.
2. main merge SHA에 연결된 OCI Production CD와 기존 Vercel Production 배포를 추적한다. CD는 기존 앱 종료→별도 Flyway→앱 시작→내부 readiness 순서다.
3. 정확한 배포 SHA·실행 결과와 공개 비파괴 smoke를 확인한다. 운영 로그인/구독/문의 데이터를 만들거나 테스트 알림을 보내지 않는다.
4. 실패 시 원인과 현재 DB 상태를 먼저 확인한다. 이전 SHA 서버 이미지와 이전 Vercel 웹을 함께 복귀해야 하며 새 세션/정상 응답 계약을 구버전이 이해한다고 가정하지 않는다. 이전 불변 이미지의 실제 사용 가능 여부를 확인한 뒤 기존 수동 배포 경로로만 실행한다.
5. V29 추가 필드는 이전 앱과 호환되고 V30은 성능 제약이므로 앱 복귀를 위해 테이블/데이터를 삭제하지 않는다. 성능 복구가 필요하면 존재 여부·DDL 잠금 비용·부분 적용을 확인한 후 문서의 인덱스 복구 SQL을 검토한다. 자동 DB 복원·clean·blind repair는 허용하지 않는다.

이 문서는 배포 전 확인 기록이다. 실제 완료 SHA·CI/CD·공개 smoke 결과는 실행 후 별도로 확인해 보고한다.
