import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, checkResponse, getRandomUserAgent } from './common-utils.js';

/**
 * [포트폴리오 시나리오] Admin Heavy Operations
 * - 시스템에서 가장 큰 자원을 소모하는 Admin 전용 API 부하 테스트.
 * - 1. 크롤링 트리거 (IO/Network Intensive)
 * - 2. 전역 통계 조회 (Aggregation Intensive)
 * - 3. 테스트 알림 발송 (External Service I/O)
 */

export const options = {
  stages: [
    { duration: '1m', target: 10 }, // 어드민 작업은 소수 인원이 강력하게 수행
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000'], // 크롤링 등은 응답 시간이 길 수 있음
    'http_req_failed': ['rate<0.05'], 
  },
};

const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || '';

export default function () {
  const params = {
    headers: {
      'User-Agent': getRandomUserAgent(),
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${ADMIN_TOKEN}`,
    },
  };

  group('Admin Heavy Tasks', function () {
    
    // 1. 전역 대시보드 통계 조회 (Aggregation)
    const statsRes = http.get(`${BASE_URL}/api/v1/admin/stats`, params);
    check(statsRes, checkResponse(statsRes, 'Admin Stats'));

    sleep(2);

    // 2. 관리자 알림 테스트 발송 (Notification Sending)
    const notificationPayload = JSON.stringify({
      email: 'test@example.com', // 실제 존재하는 유저 메일 필요
      channels: ['WEB_PUSH', 'EMAIL']
    });
    const notifyRes = http.post(`${BASE_URL}/api/v1/admin/notifications/test`, notificationPayload, params);
    check(notifyRes, checkResponse(notifyRes, 'Admin Notification Test'));

    sleep(5);

    // 3. 크롤링 트리거 (가장 무거운 작업)
    // 주의: 실제 운영 환경에서는 매우 신중히 실행해야 함
    const crawlRes = http.post(`${BASE_URL}/api/v1/admin/courses/crawl`, null, params);
    check(crawlRes, {
      'Crawl Triggered': (r) => r.status === 200 || r.status === 202,
    });

  });

  sleep(10);
}
