-- 알림 기기는 웹 푸시 구독만 저장하므로 구분 값 없이 기기 정보와 고유 토큰을 보존한다.
ALTER TABLE user_devices DROP COLUMN type;
