# 유지보수성 진단 결과 (Maintainability Findings)

## 1. 컨트롤러 도메인 다중 책임 결합 (SRP 위반)
* **위치**: [UserController.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/user/presentation/UserController.java)
* **상세**: `UserController` 클래스가 회원 가입 후 온보딩 설정, 이메일 주소 확인용 번호 전송/검증, 프로필 변경 동작뿐만 아니라 외부 서비스 연동인 디스코드 OAuth2 콜백 리다이렉트 통신 제어까지 혼재하여 처리 중입니다.
* **영향**: 회원 프로필 관리 요구사항 변경과 서드파티 OAuth 프로토콜 연동 변경 사항이 단일 컨트롤러 클래스를 동시 수정하게 만들어, 충돌 위험성과 클래스 비대화를 유발합니다.
* **조치 제안**: 외부 인증 연동에 해당하는 디스코드 OAuth 컨트롤러 레이어를 별도로 분리 추출하십시오.

## 2. NotificationService의 높은 결합도 (결합부 부채)
* **위치**: [NotificationService.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/notification/application/NotificationService.java)
* **상세**: `NotificationService` 클래스 내에 디스코드, 이메일, 웹푸시, FCM 등 모든 이종 채널 발송을 조건문 분기식으로 통제하는 하드코딩이 집약되어 있습니다.
* **영향**: 신규 알림 수단(예: 카카오톡) 추가 시 핵심 비즈니스 클래스 코드가 대대적으로 변경되어야 합니다.
* **조치 제안**: 각 발송 메커니즘을 `NotificationSender` 전략 인터페이스를 구현하는 각 채널별 스프링 빈으로 구현하여 다형성(Strategy Pattern)으로 이식할 것을 강력 제안합니다.
