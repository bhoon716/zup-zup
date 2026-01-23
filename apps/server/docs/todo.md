# User Action Items (TODO)

프로젝트 기능을 정상적으로 사용하기 위해 사용자가 직접 수행해야 하는 설정 작업들입니다.

---

## 1. Google SMTP 설정 (이메일 알림 전용)

이메일 발송 기능을 위해 Gmail의 SMTP 서버를 사용하며, 보안을 위해 **앱 비밀번호**가 필요합니다.

- [ ] **2단계 인증 활성화**: [Google 보안 설정](https://myaccount.google.com/security)에서 2단계 인증을 사용으로 설정합니다.
- [ ] **앱 비밀번호(App Password) 생성**:
  1. Google 계정 관리 -> 보안 -> 2단계 인증 메뉴로 들어갑니다.
  2. 최하단의 **앱 비밀번호**를 클릭합니다. (검색창에 '앱 비밀번호'를 입력하면 더 빠릅니다.)
  3. 앱 이름을 `SugangHelper` 등으로 입력하고 **만들기**를 클릭합니다.
  4. 생성된 **16자리 비밀번호**를 복사하여 메모해둡니다. (메일 로그인의 실제 비밀번호 대신 사용됩니다.)

---

## 2. Firebase 설정 (FCM 앱 푸시 전용)

안드로이드/iOS 앱 푸시 알림을 위해 Firebase 서비스 계정 키가 필요합니다.

- [ ] **프로젝트 생성**: [Firebase Console](https://console.firebase.google.com/)에서 프로젝트를 만듭니다.
- [ ] **서비스 계정 키(JSON) 발급**:
  1. 프로젝트 설정(톱니바퀴 아이콘) -> **서비스 계정** 탭을 선택합니다.
  2. 하단의 **새 프라이빗 키 생성** 버튼을 클릭합니다.
  3. 다운로드된 JSON 파일의 이름을 `firebase-key.json`으로 변경한 뒤, 프로젝트 폴더 내 `src/main/resources/firebase/` 하위에 위치시킵니다. (폴더가 없으면 생성하세요.)
     > [!WARNING]
     > 이 JSON 파일은 외부로 유출되지 않도록 주의해야 합니다. (사용자님의 설정에 따라 `.gitignore`에 포함되어 있습니다.)

---

## 3. Web Push (VAPID) 설정

브라우저 실시간 알림을 위해 VAPID 키 쌍이 필요합니다. 로컬에 Node.js가 설치되어 있다면 다음 명령어로 간단히 생성할 수 있습니다.

- [ ] **VAPID 키 생성**: 터미널에서 아래 명령어를 실행합니다.
  ```bash
  npx web-push generate-vapid-keys
  ```
  실행 결과로 출력되는 **Public Key**와 **Private Key**를 복사해둡니다.
- [ ] **환경 변수 매핑**: 아래 4번 항목의 `.env` 설정 시 발급받은 키를 정확히 입력합니다.

---

## 4. 최종 환경 변수 설정 (.env)

프로젝트 루트 디렉토리에 `.env` 파일을 생성(또는 수정)하고 다음 내용을 정확히 입력해주세요.

```properties
# 1. Database & Redis
DB_URL=jdbc:mysql://localhost:3306/sugang_helper?useSSL=false
DB_USERNAME=root
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379

# 2. Google OAuth2 (Google Cloud Console에서 발급)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret

# 3. Email (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_16_digits  # 위 1번에서 발급받은 16자리

# 4. Firebase (FCM)
FIREBASE_CONFIG_PATH=src/main/resources/firebase/firebase-key.json

# 5. Web Push (VAPID)
WEBPUSH_PUBLIC_KEY=your_public_key        # npx 결과의 Public Key
WEBPUSH_PRIVATE_KEY=your_private_key      # npx 결과의 Private Key
WEBPUSH_SUBJECT=mailto:admin@example.com  # 본인의 이메일 주소

# 6. Security (JWT)
JWT_SECRET=your_super_secret_key_at_least_32_characters
```

---

## 5. 방화벽 및 네트워크 확인 (Optional)

서버 환경에서 알림 발송이 실패할 경우 다음 포트의 아웃바운드 허용 여부를 확인하세요.

- **Gmail**: TCP 587 (STARTTLS)
- **FCM/WebPush**: TCP 443 (HTTPS)
