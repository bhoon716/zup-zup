import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, checkResponse, getRandomUserAgent } from './common-utils.js';

/**
 * [포트폴리오 시나리오] Notification Stress Test
 * - 대규모 여석 발생 시 알림 발송 엔진이 겪는 부하를 시뮬레이션.
 * - 외부 서비스(FCM, SMTP) 연동 및 비동기 처리 성능 검증.
 */

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // 램프 업
    { duration: '1m', target: 200 }, // 고부하 투입
    { duration: '30s', target: 0 },  // 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'], // 알림 요청 수락 응답은 빨라야 함
    'http_req_failed': ['rate<0.05'], 
  },
};

const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || '';
const TEST_EMAIL = __ENV.TEST_EMAIL || 'test@example.com';

export default function () {
  const params = {
    headers: {
      'User-Agent': getRandomUserAgent(),
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${ADMIN_TOKEN}`,
    },
  };

  const payload = JSON.stringify({
    email: TEST_EMAIL,
    channels: ['WEB_PUSH', 'EMAIL'] // 멀티 채널 발송 부하 가중
  });

  const res = http.post(`${BASE_URL}/api/v1/admin/notifications/test`, payload, params);
  
  check(res, checkResponse(res, 'Notification Stress Test'));

  sleep(Math.random() * 0.5 + 0.5); // 0.5~1초 간격으로 요청
}
