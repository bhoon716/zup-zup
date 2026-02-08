import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, checkResponse } from './common-utils.js';

/**
 * [포트폴리오 시나리오] Auth Spike Test
 * - 수강신청 오픈 직후 대규모 유저가 동시에 접속하여 토큰을 재발급받는 상황 재현.
 * - 세션 저장소(Redis) 및 인증 레이어의 병목 확인.
 */
export const options = {
  stages: [
    { duration: '10s', target: 50 },   // 준비 운동
    { duration: '20s', target: 1000 }, // 극단적인 폭증 (Spike, 1000 VUs)
    { duration: '1m', target: 1000 },  // 고부하 유지
    { duration: '30s', target: 0 },    // 급격한 감소
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200'], // 인증 속도는 매우 빨라야 함
    'http_req_failed': ['rate<0.01'], 
  },
};

export default function () {
  // 실제 시나리오에서는 유효한 세션 쿠키가 필요함
  // 여기서는 /api/auth/refresh 호출을 시뮬레이션
  const res = http.post(`${BASE_URL}/api/auth/refresh`, null, {
    headers: {
      'Cookie': 'refreshToken=dummy-token-for-spike-test', // 실제 테스트 시 유효한 토큰 필요
    },
  });

  // 더미 토큰이므로 401/403이 발생할 수 있으나, 서버 엔진의 부하 처리 능력(Response Time) 측정에 집중
  check(res, {
    'Auth Response received': (r) => r.status !== 0,
  });

  sleep(0.5); 
}
