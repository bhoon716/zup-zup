import http from 'k6/http';
import { sleep, group } from 'k6';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Endurance/Soak Test] 24/7 지속 구동 안정성 검증
 * 상황: 장기 구동 시 자원 누수(Memory Leak) 및 DB 커넥션 유지 성능을 확인하기 위해 고착화된 부하를 장시간 유지합니다.
 */
export const options = {
    vus: 500,        // 500명의 유저가 지속적으로 접속
    duration: '20m', // 20분간 테스트 진행
    thresholds: {
        'http_req_duration': ['p(95)<300'], // 지연 시간 목표 단축 (0.3초 이내)
        'http_req_failed': ['rate<0.0001'], // 장기 구동 시에도 극도로 낮은 에러율 유지
    },
};

export default function () {
    group('지속적인 기본 부하 테스트', function () {
        // 기본 강좌 목록 조회를 통해 인증 필터 및 DB 조회 성능을 지속적으로 측정합니다.
        const res = http.get(`${BASE_URL}/api/v1/courses?page=0&size=20`, getAuthHeaders());
        
        // 응답 성공 여부를 검증합니다.
        validateResponse(res, '기본강좌조회');
        
        // 유저가 페이지를 읽는 긴 시간(5초)을 시뮬레이션하여 실제 사용 패턴을 모방합니다.
        sleep(5);
    });
}
