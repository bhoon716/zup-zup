import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, checkResponse } from './common-utils.js';

/**
 * 1. Smoke Test 설정
 * 최소한의 부하(1 VU)로 스크립트 및 API 정상 여부 확인
 */
export const options = {
  vus: 1,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.01'], // 에러율 1% 미만
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내
  },
};

export default function () {
  // 1. 강의 검색 조회 (기본)
  const searchRes = http.get(`${BASE_URL}/api/v1/courses?page=0&size=10`);
  check(searchRes, checkResponse(searchRes, 'Course Search'));

  sleep(1);

  // 2. 특정 강의 상세 조회 (실제 존재하는 키 사용)
  const detailRes = http.get(`${BASE_URL}/api/v1/courses/2026:U211600010:0000100017:1`);
  check(detailRes, checkResponse(detailRes, 'Course Detail'));

  sleep(1);
}
