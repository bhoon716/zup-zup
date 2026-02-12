import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, checkResponse, getRandomUserAgent } from './common-utils.js';

/**
 * [포트폴리오 시나리오] 통합 유저 서비스 이용 플로우
 * 1. 신규/비로그인 유저: 강좌 탐색 및 상세 조회
 * 2. 기존/로그인 유저: 강좌 검색 -> 찜하기 -> 알림 구독 -> 시간표 조회
 */
export const options = {
  stages: [
    { duration: '1m', target: 200 },  // 점진적 증가
    { duration: '3m', target: 200 },  // 200명 유지
    { duration: '1m', target: 500 },  // Peak 시뮬레이션
    { duration: '5m', target: 500 }, 
    { duration: '1m', target: 0 },   // 종료
  ],
  thresholds: {
    'http_req_duration{scenario:Search}': ['p(95)<400'],
    'http_req_duration{scenario:Interaction}': ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

// 테스트용 고정 유저 정보 (실제 환경에 맞게 조정 필요)
const TEST_AUTH_TOKEN = __ENV.AUTH_TOKEN || ''; 

export default function () {
  const params = {
    headers: {
      'User-Agent': getRandomUserAgent(),
      'Content-Type': 'application/json',
    },
  };

  const authParams = {
    headers: {
      ...params.headers,
      'Authorization': `Bearer ${TEST_AUTH_TOKEN}`,
    },
  };

  // 시나리오 1: 강좌 탐색 (비로그인/로그인 공통)
  group('Course Discovery', function () {
    const searchQueries = [
      'query=컴퓨터',
      'academicYear=2026&semester=U211600010&generalCategory=교양',
      'query=홍길동', // 교수명 검색
    ];
    
    const query = searchQueries[Math.floor(Math.random() * searchQueries.length)];
    // URL 인코딩 처리 (한글 검색어 등 대비)
    const encodedQuery = query.split('&').map(pair => {
      const [key, value] = pair.split('=');
      return `${key}=${encodeURIComponent(value)}`;
    }).join('&');

    const url = `${BASE_URL}/api/v1/courses?${encodedQuery}&page=0&size=20`;
    console.log(`[k6] Requesting URL: ${url}`);
    
    const res = http.get(url, params, { tags: { scenario: 'Search' } });
    console.log(`[k6] Response Status: ${res.status}`);
    
    check(res, checkResponse(res, 'Course Search'));
    
    sleep(2); // 생각 시간

    if (res.status === 200) {
      console.log(`[k6] Course search success, parsing body...`);
      const body = JSON.parse(res.body);
      if (body.data.content && body.data.content.length > 0) {
        const courseKey = body.data.content[0].courseKey;
        const detailRes = http.get(`${BASE_URL}/api/v1/courses/${courseKey}`, params);
        check(detailRes, checkResponse(detailRes, 'Course Detail'));
        
        const historyRes = http.get(`${BASE_URL}/api/v1/courses/${courseKey}/history`, params);
        check(historyRes, checkResponse(historyRes, 'Course Seat History'));
      }
    }
  });

  sleep(3);

  // 시나리오 2: 로그인 유저 플로우 (인증 필요)
  if (TEST_AUTH_TOKEN) {
    group('User Engagement (Authenticated)', function () {
      const targetCourseKey = '2026:U211600010:0000100017:1'; // 유효한 키 고정 사용

      // 1. 찜하기 토글 (Write)
      const wishRes = http.post(`${BASE_URL}/api/v1/wishlist/${targetCourseKey}`, null, { ...authParams, tags: { scenario: 'Interaction' } });
      check(wishRes, checkResponse(wishRes, 'Toggle Wishlist'));

      sleep(1);

      // 2. 시간표 신규 생성 (Write-Heavy)
      const timetablePayload = JSON.stringify({ name: '2026-1학기 기본', academicYear: '2026', semester: 'U211600010' });
      const createTimetabelRes = http.post(`${BASE_URL}/api/v1/timetables`, timetablePayload, authParams);
      check(createTimetabelRes, checkResponse(createTimetabelRes, 'Create Timetable'));

      if (createTimetabelRes.status === 200) {
        const timetableId = JSON.parse(createTimetabelRes.body).data.id;
        
        // 3. 시간표에 강좌 추가 (Write-Heavy)
        const addCoursePayload = JSON.stringify({ courseKey: targetCourseKey });
        const addRes = http.post(`${BASE_URL}/api/v1/timetables/${timetableId}/courses`, addCoursePayload, authParams);
        check(addRes, checkResponse(addRes, 'Add Course to Timetable'));
        
        sleep(1);
      }

      // 4. 알림 구독 신청 (Write)
      const subPayload = JSON.stringify({ courseKey: targetCourseKey });
      const subRes = http.post(`${BASE_URL}/api/v1/subscriptions`, subPayload, authParams);
      check(subRes, checkResponse(subRes, 'Subscribe Notification'));

      sleep(2);

      // 5. 내 시간표 목록 최종 확인 (Read)
      const finalTimetableRes = http.get(`${BASE_URL}/api/v1/timetables`, authParams);
      check(finalTimetableRes, checkResponse(finalTimetableRes, 'Get My Timetables'));

      sleep(1);

      // 6. 알림 수신 히스토리 조회 (Reading historical data)
      const historyRes = http.get(`${BASE_URL}/api/v1/notifications/history`, authParams);
      check(historyRes, checkResponse(historyRes, 'Get Notification History'));
    });
  }

  sleep(5);
}
