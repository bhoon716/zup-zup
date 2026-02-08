import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, checkResponse, getRandomUserAgent } from './common-utils.js';

/**
 * 2. Load Test 설정
 * 예상 트래픽 하에서의 안정성 및 응답 속도 검증
 */
export const options = {
  stages: [
    { duration: '1m', target: 100 }, // 100명까지 증가
    { duration: '3m', target: 100 }, // 100명 유지
    { duration: '1m', target: 200 }, // 200명까지 증가 (Peak)
    { duration: '3m', target: 200 }, // 200명 유지
    { duration: '2m', target: 0 },   // 종료
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300', 'p(99)<800'],
  },
};

export default function () {
  const params = {
    headers: {
      'User-Agent': getRandomUserAgent(),
    },
  };

  group('Course Discovery Flow', function () {
    // 1. 전체 강의 목록 조회 (랜덤 페이지)
    const page = Math.floor(Math.random() * 5);
    const listRes = http.get(`${BASE_URL}/api/v1/courses?page=${page}&size=30`, params);
    check(listRes, checkResponse(listRes, 'List Courses'));

    sleep(Math.random() * 2 + 1); // 1~3초 생각 시간

    // 2. 특정 조건 검색 (교양/전공 등 - 실제 데이터에 따라 파라미터 다양화 가능)
    const searchRes = http.get(`${BASE_URL}/api/v1/courses?academicYear=2026&semester=U211600010&page=0&size=30`, params);
    check(searchRes, checkResponse(searchRes, 'Filtered Search'));

    sleep(Math.random() * 3 + 2); // 2~5초 생각 시간
  });
}
