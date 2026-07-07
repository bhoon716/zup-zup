import http from 'k6/http';
import { sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Data-driven Load Test] 다양한 조건의 강좌 검색 부하 검증
 * 상황: 실제 사용자들이 입력할 만한 다양한 검색 필터를 사용하여 DB 인덱싱 및 필터링 연산 성능을 측정합니다.
 */
const filters = new SharedArray('검색 필터 데이터', function () {
    return [
        { query: '사회적가치와개인의자유', academicYear: '2026' },
        { query: '고사와성어의탐구', semester: '1' },
        { generalCategory: '일반선택', credits: 2 },
        { department: '간호학', targetGrade: 1 },
        { professor: '외부교원1' },
        { query: 'AI를활용한음악만들기', professor: '외부교원1' },
        { department: '전체(학부)', academicYear: '2026' },
        { query: '글로벌시대의세계시민' }
    ];
});

export const options = {
    stages: [
        { duration: '30s', target: 500 },  // 점진적 부하 증가
        { duration: '2m', target: 1500 }, // 1500명의 유저가 반복적으로 검색 수행
        { duration: '1m', target: 1500 }, 
        { duration: '30s', target: 0 },    // 부하 종료
    ],
    thresholds: {
        'http_req_duration': ['p(99)<1000'], // 99%의 요청이 1초 이내에 응답받아야 함
    },
};

export default function () {
    group('다양한 필터 검색 테스트', function () {
        // 정의된 필터 목록 중 하나를 무작위로 선택하여 시뮬레이션합니다.
        const filter = filters[Math.floor(Math.random() * filters.length)];
        
        // 검색 필터 객체를 HTTP 쿼리 파라미터 문자열로 변환합니다.
        const queryParams = Object.keys(filter)
            .map(key => `${key}=${encodeURIComponent(filter[key])}`)
            .join('&');

        // 인증 필터 부하를 포함하여 강좌 목록을 검색합니다.
        const res = http.get(`${BASE_URL}/api/v1/courses?${queryParams}`, getAuthHeaders());
        
        // 검색 결과 응답이 성공적인지 확인합니다.
        validateResponse(res, '필터검색');

        // 극단적인 부하를 주기 위해 매우 짧은 생각 시간(0.1~0.3초)을 시뮬레이션합니다.
        sleep(Math.random() * 0.2 + 0.1);
    });
}
