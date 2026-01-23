# User Action Items (TODO)

## 1. Google SMTP 설정 (Email Notification)

이메일 알림 기능을 활성화하기 위해 다음 설정이 필요합니다.

- [ ] **Google 계정 2단계 인증 설정**: [Google 보안 설정](https://myaccount.google.com/security)에서 2단계 인증을 켭니다.
- [ ] **앱 비밀번호 생성**:
  1.  Google 계정 관리 -> 보안 -> 2단계 인증 -> 하단 **앱 비밀번호(App Passwords)** 선택.
  2.  앱 선택: "메일", 기기 선택: "Mac" (또는 기타).
  3.  생성하기 버튼 클릭.
  4.  **16자리 비밀번호** (공백 없이)를 복사해둡니다.

## 2. 환경 변수 설정 (.env)

프로젝트 루트의 `.env` 파일에 다음 내용을 추가해주세요.

```properties
# Email Notification (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_16_digits
MAIL_AUTH=true
MAIL_STARTTLS_ENABLE=true
```

- `MAIL_USERNAME`: 알림을 발송할 Gmail 주소
- `MAIL_PASSWORD`: 위에서 생성한 16자리 앱 비밀번호

## 3. 방화벽 확인 (Optional)

- 로컬 개발 환경이나 배포 서버에서 **587 포트 (SMTP)** 아웃바운드 트래픽이 허용되어 있는지 확인이 필요할 수 있습니다. (대부분의 클라우드 환경에서는 기본 허용됨)
