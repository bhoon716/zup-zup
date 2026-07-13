# 알림 delivery·DLQ 운영 기준

시행: 2026-07-13. 이 문서는 `ISSUE-088`의 at-least-once delivery 계약이다.

## 전달 보장 범위

- 좌석 알림은 **at-least-once**다. provider 요청 직후 네트워크 결과가 불명확하면 같은 delivery를 재시도할 수 있으므로, 이메일·FCM·Web Push는 수신자가 중복을 볼 가능성이 남는다.
- delivery마다 DB에 안정적인 UUID idempotency key를 한 번만 만들고 retry와 replay에서 그대로 유지한다. 원본 key와 수신자, provider 예외 원문은 API·관리자 화면·감사 로그에 저장하거나 반환하지 않는다.
- worker는 claim token으로 lease 소유자를 확인한다. provider 호출은 DB 트랜잭션 밖에서 수행하고, 완료/실패 반영 시에도 같은 token을 확인하므로 lease를 잃은 worker는 새 상태를 덮어쓰지 못한다. 이 fencing은 중복 상태 반영을 막지만, 이미 provider에 전달된 요청을 취소하지는 못한다.
- 한 delivery가 여러 기기로 fan-out될 때 한 기기 성공 뒤 다른 기기 실패가 나면 성공한 기기도 다시 요청될 수 있다. client key 처리로 UI 중복을 줄이며, target별 정확한 성공 기록은 후속 fan-out 개선 범위다.

## Provider별 key 전파

- 원본 UUID는 SHA-256 기반의 22자 URL-safe provider key로 변환해 외부 provider에 보낸다. 외부 topic/nonce에는 user ID, 수신자, 원본 key를 쓰지 않는다.
- Discord는 `nonce`와 `enforce_nonce=true`를 보낸다. Discord는 같은 author/nonce의 최근 몇 분 요청을 기존 메시지로 돌려주므로 그 창에서는 provider 중복 생성을 막는다. [Discord Create Message 문서](https://docs.discord.com/developers/resources/message)
- Web Push는 같은 provider key를 `Topic`과 encrypted payload에 넣고, service worker는 payload key를 notification `tag`로 사용해 재표시를 교체한다. Topic은 아직 전달되지 않은 메시지 대체 수단일 뿐, 이미 전달된 메시지의 정확한 한 번 표시를 보장하지 않는다. [RFC 8030 §5.4](https://datatracker.ietf.org/doc/rfc8030/)
- FCM은 custom data의 `idempotencyKey`를 전달한다. FCM client는 이 key를 최근 수신 기록과 비교해 중복 UI를 건너뛰어야 하며, 현재 Android/iOS client의 durable dedup은 별도 구현 대상이다. [FCM data message 문서](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type)
- Email은 `X-Idempotency-Key` header를 상관관계 식별자로만 넣는다. SMTP/provider dedup 보장은 아니므로 중복 가능성을 없애지 않는다.

## DLQ 조회와 선택 재처리

- `GET /api/v1/admin/notification-deliveries/dlq`와 `GET /api/v1/admin/notification-deliveries/{id}`는 `ROLE_ADMIN`만 사용한다. 현재 단일 관리자 운영 정책에 따라 ownership 필터 없이 전체 delivery를 본다.
- `POST /api/v1/admin/notification-deliveries/{id}/replay`는 root cause를 고친 뒤 선택한 delivery 하나만 재처리한다. DLQ는 기본 허용하고 동일 delivery ID/key를 유지하며 retry budget을 새로 시작한다.
- `SENT`는 기본 재처리 대상이 아니다. API의 명시적 `forceSentReplay: true`일 때만 허용하며, 관리자 화면은 이 override를 노출하지 않는다.
- 성공한 replay는 이전 상태와 시도 횟수, key 유지 사실만 `DELIVERY_REPLAY` 감사 로그로 남긴다.

## 보존

- DLQ는 실제 DLQ 전환 시각(`dead_lettered_at`) 기준으로 최소 30일 보존한다. 매일 03:30 정리하되 `APP_NOTIFICATION_OUTBOX_DLQ_RETENTION_DAYS`가 30보다 작아도 30일로 보정한다.
- 기존 DLQ row는 V19 적용 시점부터 30일을 새로 보장한다.
