import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, checkResponse } from './common-utils.js';

/**
 * 3. Stress Test 설정
 * 시스템이 버틸 수 있는 최대 부하 지점 도출
 */
export const options = {
  stages: [
    { duration: '2m', target: 50 },  // 서서히 부하 증가
    { duration: '3m', target: 100 }, // 부하 가속
    { duration: '5m', target: 200 }, // 한계치 투입
    { duration: '2m', target: 0 },   // 복구 및 종료
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'], // 스트레스 테스트 시에는 5%까지 허용
    http_req_duration: ['p(95)<1000'], // 1초 이내 응답 목표
  },
};

export default function () {
  // 인증이 필요 없는 과목 검색 API 위주로 대량 호출
  const res = http.get(`${BASE_URL}/api/v1/courses?page=0&size=50`);
  check(res, checkResponse(res, 'Stress Test Search'));

  sleep(0.5); // 고집적 요청을 위해 짧은 대기 시간
}
