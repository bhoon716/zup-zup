# 관리자 감사 로그 운영 기준

시행: 2026-07-13. 이 문서는 `ISSUE-085`의 운영 계약이다.

## 조회 범위

- `GET /api/v1/admin/audit-logs`는 `ROLE_ADMIN`만 사용할 수 있다. 관리자는 페이지를 통해 모든 감사 로그를 조회할 수 있다.
- 응답에는 관리자 ID, 액션·대상·시각과 최소화된 metadata만 포함한다. 관리자 이메일, 답변 원문, 피드백 본문, 첨부파일 이름·URL, token, 수신자, idempotency key, 예외 원문 및 IP는 저장하거나 반환하지 않는다.
- metadata는 `schema: "admin-action"`, `version: 1` envelope를 사용한다. 답변 변경은 길이와 키 기반 HMAC-SHA-256 지문만, 상태 변경은 이전/이후 상태만 기록한다. 운영에서는 독립된 `APP_ADMIN_AUDIT_FINGERPRINT_SECRET` 설정을 권장한다.

## 보존과 정리

- 최소화된 감사 로그의 기본 보존 기간은 180일이다. 서버 로컬 시각 기준 매일 03:15에 정리하며 `APP_ADMIN_AUDIT_RETENTION_DAYS`와 `APP_ADMIN_AUDIT_RETENTION_CRON`으로 조정할 수 있다. 기간을 `0` 이하로 설정하면 자동 정리는 중지된다.
- 이 기간은 운영 조사에 필요한 한 학기 단위의 기본값이며 법정 보존 기간을 주장하지 않는다. 피드백 본문·첨부파일의 no-auto-purge 정책과 별개다.
- V18은 현재 DB의 기존 답변 metadata를 `LEGACY_SANITIZED` envelope로 바꿔 과거 답변 원문 복제를 제거한다. enum 값만 있는 기존 상태 변경 이력은 v1 상태 전환으로 보존한다. 기존 백업은 자체 보존·순환 정책에 따라 별도로 만료시켜야 한다.

## 후속 연결

- 상태 변경, 답변 생성·수정·삭제, 관리자 첨부파일 접근은 동일한 감사 모델로 기록한다. 첨부파일은 HTTP 스트리밍 완료가 아니라 권한 확인과 리소스 해석이 성공한 **접근**을 기록한다.
- DLQ 선택 재처리(`ISSUE-088`)는 성공한 상태 전이와 같은 transaction에서 `DELIVERY_REPLAY`를 기록한다. 이전 delivery 상태·기존 시도 횟수·key 유지 사실만 남기며 원본 key, 수신자, provider 예외 원문은 남기지 않는다.
