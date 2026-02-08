import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, checkResponse } from './common-utils.js';

/**
 * [포트폴리오 시나리오] 고강도 Stress Test (Soak/Spike 대응)
 * - 시스템의 Saturation Point를 도출하고 장애 시 에러 전파 양상 관찰.
 */
export const options = {
  stages: [
    { duration: '1m', target: 200 },
    { duration: '2m', target: 500 },
    { duration: '2m', target: 800 },
    { duration: '3m', target: 1000 }, // 한계 투입 (1000 VUs)
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'], // 장애 임계치 5% 설정
    http_req_duration: ['p(95)<1500'], // 고부하 시 아웃라이어 허용 범위
  },
};

export default function () {
  // 가장 무거운 전체 검색 + 페이징 API 집중 타격
  const params = {
    tags: { name: 'HeavyCourseSearch' },
  };

  const res = http.get(`${BASE_URL}/api/v1/courses?page=0&size=50`, params);
  
  check(res, checkResponse(res, 'Heavy Load Search'));

  // 부하 집중을 위해 sleep 최소화
  sleep(0.1); 
}
