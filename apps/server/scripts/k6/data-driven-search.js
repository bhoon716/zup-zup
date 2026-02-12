import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, checkResponse } from './common-utils.js';

/**
 * [포트폴리오 시나리오] 데이터 기반 검색 성능 테스트 (Data-driven)
 * - 고정된 쿼리가 아닌 다양한 검색 파라미터를 사용하여 DB 캐시 히트를 방지하고 실제 연산 성능 측정.
 */

// 1. 검색 데이터 로드 (실제 운영 데이터 샘플링 권장)
const searchData = new SharedArray('search parameters', function () {
  return [
    { academicYear: '2026', semester: 'U211600010', query: '알고리즘' },
    { academicYear: '2026', semester: 'U211600010', query: '데이터' },
    { academicYear: '2026', semester: 'U211600010', generalCategory: '교양', generalDetail: '인문' },
    { academicYear: '2025', semester: 'U211600020', minCredits: 3 },
    { academicYear: '2026', semester: 'U211600010', lectureHours: 3 },
  ];
});

export const options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'],
  },
};

export default function () {
  // 랜덤하게 검색 조건 선택
  const data = searchData[Math.floor(Math.random() * searchData.length)];
  
  // 쿼리 스트링 빌드
  const queryString = Object.keys(data)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    .join('&');

  const res = http.get(`${BASE_URL}/api/v1/courses?${queryString}&page=0&size=30`);
  
  check(res, checkResponse(res, `Data-driven Search: ${data.query || data.generalCategory || 'Mixed'}`));

  sleep(Math.random() * 1 + 0.5); // 0.5~1.5초 간격
}
