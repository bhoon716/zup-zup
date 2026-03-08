import http from 'k6/http';
import { sleep, group } from 'k6';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Spike Test] 알림 유입 기반 순간 폭주 검증
 * 상황: 여석 발생 알림 발송 직후 수 초 이내에 전교생 수준의 대규모 접속이 발생하는 상황을 시뮬레이션합니다.
 */
export const options = {
    stages: [
        { duration: '10s', target: 500 },  // 초기 유입 시작
        { duration: '30s', target: 5000 }, // 최단 시간에 5000명까지 폭주
        { duration: '1m', target: 5000 },  // 최고 부하 상태 유지
        { duration: '30s', target: 0 },    // 부하 종료 및 정상화
    ],
    thresholds: {
        'http_req_duration': ['p(95)<300'], // 대부분의 요청이 0.3초 이내에 완료되어야 함
        'http_req_failed': ['rate<0.01'],    // 전체 에러율은 1% 미만으로 유지
    },
};

export default function () {
    group('알림 유입 폭주 시뮬레이션', function () {
        // 인증 풀에서 자동 선택된 토큰을 사용하여 알림 히스토리를 조회합니다.
        const res = http.get(`${BASE_URL}/api/v1/notifications/history`, getAuthHeaders());
        
        // 응답 상태 코드(200) 및 본문 유효성을 검증합니다.
        validateResponse(res, '알림히스토리');
        
        // 실제 사용자의 페이지 체류 및 생각 시간(0.5~1.5초)을 시뮬레이션합니다.
        sleep(Math.random() * 1 + 0.5);
    });
}
