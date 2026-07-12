# 탈퇴·보존 운영 기준

시행: 2026-07-13. 이 문서는 `ISSUE-082`의 운영 계약이다.

## 탈퇴 시 처리

| 데이터 | 처리 | 일반 사용자 접근 |
| --- | --- | --- |
| 이름, 로그인·알림 이메일, Discord 식별자 | 즉시 익명화 또는 제거 | 차단 |
| access/refresh 인증, HTTP session, device token | 즉시 폐기 | 차단 |
| 계정 row | `deleted_at` soft delete | 차단 |
| 구독 | row 보존, 비활성화 | 알림 발송 제외 |
| 시간표, 찜, 리뷰·반응, 알림·outbox 이력 | 비식별 `user_id`와 함께 보존 | 탈퇴 계정에서는 차단 |
| feedback 본문·첨부파일 | feedback soft delete, 파일 보존 | 일반 경로에서 숨김 |

단일 관리자에게 보존 데이터의 전체 조회를 허용하는 정책이다. soft-deleted feedback의 목록·감사 화면은 `ISSUE-083`에서 제공한다. 피드백 본문·첨부파일에는 사용자가 입력한 개인정보가 포함될 수 있으므로, 이 보존은 개인정보·법률 검토가 필요한 수용 위험이다.

## 인증과 재가입

- access/refresh token과 HTTP session은 불변 `userId`를 함께 검증한다. 탈퇴 전 인증은 같은 이메일로 새 계정이 생겨도 새 계정으로 해석되지 않는다.
- 같은 Google 이메일로 다시 로그인하면 기존 탈퇴 row를 복구하지 않고 새 계정을 만든다.
- `uid` claim 또는 session user ID가 없는 배포 전 인증은 안전하게 구별할 수 없으므로 fail-closed한다. 배포 직후 기존 로그인은 한 번 다시 인증해야 한다.

## 운영 runbook

1. 탈퇴 요청 성공 후 refresh cookie와 현재 HTTP session이 만료됐는지 확인한다.
2. 알림 사고 조사 시 해당 `user_id`의 구독이 비활성이고 device row가 없는지 확인한다. 이미 외부 provider 호출을 시작한 발송은 완전 취소할 수 없어 best-effort 경계다.
3. 법률·개인정보 검토 전에는 자동 또는 임의 purge를 실행하지 않는다. 보존 기간·최종 파기 기준이 확정되면 별도 migration/runbook과 함께 추가한다.
4. `users.deleted_at`과 immutable `users.id`는 초기 schema에 이미 있다. 다만 동시 탈퇴·프로필 변경이 계정을 되살리지 않도록 V17은 기존 `users` row에 `version = 0`을 backfill하는 optimistic-lock column을 추가한다. 배포 전 `./gradlew migrationTest`로 upgrade path를 확인한다.
