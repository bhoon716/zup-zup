# 알림 채널별 멱등성과 중복 억제 조사

- 조사일: 2026-10-01
- 범위: 이메일·Discord·웹 푸시의 공급자 계약과 현재 코드
- 상태: 공식 문서와 코드 조사. 실제 공급자 요청·브라우저 실험·운영 환경 확인은 수행하지 않았다.
- 합의한 정책: [중복 허용·감지 시각 표시·MQ 도입 범위](decisions/2026-10-01-notification-duplicate-and-mq-policy.md)

## 1. 요약

| 채널 | 공급자·브라우저 지원 | 현재 코드 | 남는 중복 가능성 |
| --- | --- | --- | --- |
| 이메일 | SES 공개 발송 API에서 멱등성 키 계약을 확인하지 못함 | JavaMail로 발송하며 `X-Idempotency-Key` 헤더 추가 | 헤더를 넣었다는 사실만으로 공급자의 중복 차단을 보장할 수 없음 |
| Discord | `nonce`와 `enforce_nonce`로 최근 몇 분의 같은 작성자·키 중복 생성 억제 | 두 필드 모두 사용 | 적용 시간이 지난 재시도 |
| 웹 푸시 서비스 | 같은 구독의 같은 `Topic`인 대기 메시지 교체 | `Topic` 사용 | 앞선 메시지가 이미 전달된 뒤 재시도 |
| 웹 푸시 화면 | 같은 출처·`tag`의 기존 알림 교체 | `tag`, `renotify=false` 사용 | 기존 알림을 닫은 뒤 재수신, 별도의 인앱 토스트 |

각 계약의 근거는 아래 채널별 공식 문서에 연결했다. **키를 전송하는 것, 공급자가 중복 요청을 차단하는 것, 사용자의 화면에서 알림이 한 번만 보이는 것은 서로 다른 보장이다.**

## 2. 우리 DB의 멱등성으로 막을 수 있는 범위

현행 [발송 작업](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/domain/SeatNotificationDelivery.java)은 `outbox_id + user_id + channel` 유일 제약으로 같은 outbox의 같은 사용자·채널 작업 중복 생성을 막는다. 작업 생성 시 UUID 키를 저장하고 재시도·관리자 재처리에서도 유지한다. 작업 점유 때 새로 만드는 `claimToken`은 이 키와 별개다.

[공급자 키 변환](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/NotificationDeliveryIdempotencyKey.java)은 저장된 키를 SHA-256·Base64 URL 형식의 22자로 바꾼다. 현재 발송 경로에서는 같은 작업이 같은 공급자 키를 사용한다. 이는 공급자 기능을 사용할 준비이며, 공급자별 실제 보장을 추가하지는 않는다.

남는 실패 구간은 다음과 같다.

1. 외부 서비스가 알림 발송 요청을 처리한다.
2. 응답 유실 또는 서버 중단으로 우리 DB에 성공을 기록하지 못한다.
3. 우리 서버가 같은 작업을 재시도한다.
4. 외부 서비스가 키를 지원하지 않거나 적용 범위를 벗어나면 중복 발송될 수 있다.

MQ 소비자의 처리 기록도 외부 서비스 호출과 원자적으로 묶이지 않으면 같은 구간이 남는다. 처리 전에 완료를 기록하면 중단 시 누락 위험이 생긴다. 따라서 MQ 도입만으로 이 문제를 해결한다고 평가하지 않는다.

## 3. 이메일

### 확인한 코드와 계약

- [EmailNotificationSender](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/infra/EmailNotificationSender.java)는 `JavaMailSender`로 메시지를 보내고 임의 헤더 `X-Idempotency-Key`를 설정한다. 이 헤더를 조회해 중복을 차단하는 코드는 없다.
- [운영 설정](../apps/server/src/main/resources/application-prod.yml)은 SES를 활성화하며 [의존성](../apps/server/build.gradle)에 Spring Cloud AWS SES가 있다. 기본 설정은 로컬 SMTP를 사용한다. 실제 운영에서 선택되는 `JavaMailSender` 구현과 발송 경로는 실행 환경에서 확인할 사항이다.
- SES의 공개 [SendRawEmail](https://docs.aws.amazon.com/ses/latest/APIReference/API_SendRawEmail.html)과 [SendEmail v2](https://docs.aws.amazon.com/ses/latest/APIReference-V2/API_SendEmail.html) 요청 명세에서 멱등성 키나 `ClientToken` 기반 중복 차단 계약을 확인하지 못했다. `X-Idempotency-Key`에 중복 차단 의미를 부여하는 근거도 확인하지 못했다.
- SES `SendRawEmail`은 발신자가 지정한 `Message-ID`와 `Date`를 자체 값으로 덮어쓴다. 같은 `Message-ID`를 넣으면 SES가 발송을 한 번만 수행한다는 방식으로 설계할 수 없다. [SES 명세](https://docs.aws.amazon.com/ses/latest/APIReference/API_SendRawEmail.html)

### 추가 선택지와 비용

SES를 유지한다면 성공이 기록된 작업의 재발송을 막을 수 있지만, 성공 여부가 불확실한 발송을 재시도할 때의 중복은 남는다. 헤더는 상관관계 추적에 활용할 여지가 있으며 멱등성 보장으로 설명하면 안 된다.

공급자 차이를 보여주는 사례로 Resend는 API의 `Idempotency-Key`와 SMTP의 `Resend-Idempotency-Key`를 지원하고 키를 24시간 보관한다. 같은 키의 재요청에서 다시 보내지 않는 계약이 있다. 해당 시간 이후까지 중복 방지를 보장하지는 않는다. [Resend 공식 문서](https://resend.com/docs/dashboard/emails/idempotency-keys)

이는 공급자 교체를 결정한 내용이 아니다. 사용자 요구가 바뀌어 이메일 중복 억제가 중요해지면 비용·발송 품질·전환 작업과 함께 비교할 수 있다. 현재 중복 허용 정책에서 교체가 필수라는 근거는 없다.

## 4. Discord

### 확인한 계약

`Create Message`의 `nonce`는 최대 25자다. `enforce_nonce=true`를 함께 넣으면 최근 몇 분 내 같은 작성자·같은 nonce의 메시지가 있을 때 기존 메시지를 반환하고 새 메시지를 만들지 않는다. 공식 문서는 정확한 분 수를 명시하지 않으므로 재시도 안전 기간을 임의로 5분 등으로 확정할 수 없다. [Discord 공식 명세](https://docs.discord.com/developers/resources/message#create-message)

### 현재 구현과 의미

[DiscordNotificationSender](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/infra/DiscordNotificationSender.java)는 봇의 DM 메시지 생성 요청에 두 필드를 이미 넣고 있다. 현재 22자 공급자 키는 길이 제한 안에 있다.

같은 작업의 짧은 간격 재시도에는 공급자 중복 억제를 활용할 수 있다. 긴 장애 뒤 재시도·관리자 재처리에서는 중복이 남을 수 있다. 다시 만석이 됐다가 새 여석이 생긴 사건은 새 작업·키로 구분해야 한다. 강의 ID만 nonce로 사용하면 실제 새 알림까지 억제할 수 있다.

## 5. 웹 푸시

### 푸시 서비스의 Topic

RFC 8030의 `Topic`은 **같은 구독에서 아직 대기 중인 메시지**를 교체한다. 키는 최대 32자의 URL 안전 Base64 문자다. 이미 전달된 메시지를 기억해서 모든 재요청을 막는 계약은 아니다. 또한 푸시 서비스의 HTTP 201은 요청 수락이며 브라우저 전달 완료를 의미하지 않는다. [RFC 8030 §5 및 §5.4](https://www.rfc-editor.org/rfc/rfc8030#section-5.4)

[WebPushNotificationSender](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/infra/WebPushNotificationSender.java)는 같은 공급자 키를 `Topic`과 암호화된 payload의 `idempotencyKey`에 넣는다. 현재 22자 키는 Topic 제약에 맞는다. 기기가 오프라인일 때 같은 작업의 대기 메시지가 쌓이는 것을 줄일 수 있다. 서로 다른 여석 발생의 키까지 같게 만들면 전달할 발생 사실이 교체될 수 있으므로 구분이 필요하다.

### 브라우저 알림의 tag와 인앱 토스트

Notifications API의 `tag`는 같은 출처·tag의 기존 알림을 교체하는 장치다. 처리한 모든 키를 영구히 기억하는 장치가 아니다. 기존 알림이 닫혀 목록에서 제거되면 같은 키를 다시 받아도 교체할 알림이 없을 수 있다. [Notifications API 표준](https://notifications.spec.whatwg.org/#showing-a-notification)

[서비스 워커](../apps/web/public/sw.js)는 payload 키를 `tag`에 넣고 `renotify=false`를 설정한다. 이는 교체 시 재알림을 억제하려는 설정이며 푸시 요청이나 `push` 이벤트를 한 번으로 만드는 기능은 아니다. 실제 표시·소리 동작은 지원 브라우저·OS에서 별도 확인해야 한다.

서비스 워커는 매번 열린 페이지에 `PUSH_NOTIFICATION`을 전달하지만 이 메시지에 멱등성 키는 포함하지 않는다. [providers.tsx](../apps/web/src/app/providers.tsx)는 매번 토스트를 띄운다. 따라서 OS 알림이 교체돼도 인앱 토스트는 중복될 수 있다.

### 추가 후보: 클라이언트의 처리 키 기록

웹 화면의 추가 억제가 필요하면 다음 방식을 비교할 수 있다. 아직 채택한 설계가 아니다.

| 후보 | 억제 범위 | 비용·한계 |
| --- | --- | --- |
| 페이지에 키를 전달하고 토스트 ID로 사용 | 페이지에서 같은 작업의 토스트 중복 감소 | 새로고침·탭·보관 범위를 정해야 함 |
| 서비스 워커에서 처리 키를 IndexedDB 등에 보관 | 알림을 닫은 뒤 재수신까지 추가 억제 가능 | 보관·정리·동시 처리·저장 실패·사용자 데이터 초기화 고려 필요 |

처리 기록을 먼저 남기고 표시 전에 중단되면 누락, 표시 후 기록 전에 중단되면 중복 가능성이 있다. 클라이언트 저장과 OS 표시도 하나의 트랜잭션이 아니므로 완전한 한 번 표시 보장으로 설명하지 않는다. 현재 중복 허용 요구에서 영속 저장소가 필수라고 판단할 근거는 없다.

## 6. 감지 시각: 합의한 요구와 현행 구현의 차이

[수집 반영 코드](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/application/CourseCatalogSynchronizationService.java)는 만석에서 여석으로 전환되면 이벤트를 발행한다. [SeatOpenedEvent](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/contract/SeatOpenedEvent.java)와 [outbox](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/domain/SeatNotificationOutbox.java)에는 명시적인 감지 시각 필드가 없다. outbox의 감사용 `createdAt`과 서비스 워커의 `dateOfArrival`은 각각 기록 생성·도착 시각이므로 감지 시각과 같다고 간주할 수 없다.

[메시지 생성](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/application/NotificationService.java)은 현재 여석 수와 강의 정보만 사용한다. 감지 시각과 수집·발송 지연 안내는 아직 구현되지 않았다.

구현할 때 최초 감지 시각을 발송 데이터에 보존하고 재시도에도 사용해야 한다. 정확히 어느 수집 단계의 시각인지 함께 정해야 하며, 학교의 실제 여석 발생 시각이라고 표현하지 않는다. 표시 시각은 설명 정보이며 초 단위 시각 자체를 유일한 멱등성 키로 쓰지 않는다.

메시지 초안은 다음과 같다. 날짜·KST 표기를 포함하면 다음 날 도착해도 해석할 수 있다. 최종 문구·표시 형식은 미정이다.

> 2026-10-01 14:32:08 (한국 시간)에 여석 1명을 감지했습니다.
> 수집 주기와 알림 전달 지연 등으로 현재 여석 상태와 다를 수 있습니다. 수강 신청 페이지에서 확인해 주세요.

## 7. 다음 논의에 사용할 제안

- 같은 발생·수신자·채널 작업은 재시도에서도 같은 키를 사용하고 새 발생은 구분한다. 현재 테이블 구조를 그대로 채택할지는 별도 논의한다.
- Discord와 웹 푸시의 기존 억제 기능은 각각의 범위로 설명한다. 이메일의 임의 헤더를 중복 방지 보장으로 취급하지 않는다.
- 원래 감지 시각 기록·표시를 구현 범위에 넣는다.
- 이메일 공급자 교체·클라이언트 영속 처리 기록은 현재 요구만으로 확정하지 않는다.
- 재시도 횟수·간격·기간, 성공 판정, 여러 기기 중 일부 실패, 오래된 알림과 관리자 재처리는 사용자와 이어서 정한다.

이번 조사에서는 애플리케이션 코드를 수정하거나 테스트를 추가·실행하지 않았다.
