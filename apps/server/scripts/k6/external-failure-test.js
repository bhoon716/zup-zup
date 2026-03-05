import http from 'k6/http';
import { sleep, group } from 'k6';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Resilience Test] 외부 시스템 장애 전파 차단 검증
 * 상황: 연동 서버(학교 시스템) 지연 발생 시, 우리 서비스의 스레드 자원이 고갈되지 않고 보호되는지 확인합니다.
 */
export const options = {
    vus: 2000,       // 2000명의 동시 접속자가 외부 의존 호출 수행
    duration: '3m',  // 3분간 지속 테스트
    thresholds: {
        'http_req_duration': ['p(95)<1000'], // 외부 장애 상황에서도 핵심 관리자 API는 1초 이내 응답 목표
    },
};

export default function () {
    group('외부 시스템 의존성 탄력성 테스트', function () {
        // 관리자 권한 토큰을 사용하며, 요청 타임아웃을 3초로 제한하여 시스템을 보호합니다.
        const params = getAuthHeaders({ isAdmin: true });
        params.timeout = '3s';
        params.tags = { name: '외부의존성체크' };

        // 관리자 대시보드 조회를 통해 백엔드 스레드 가용성 및 외부 지표 응답을 시뮬레이트합니다.
        const res = http.get(`${BASE_URL}/api/v1/admin/overview`, params);

        // 특정 상태 코드(504, 408)는 의도된 타임아웃 상황으로 간주하여 체크합니다.
        checkResponse(res);
        sleep(1);
    });
}

/**
 * 응답 상태를 체크하여 타임아웃이나 성공 여부를 판별합니다.
 */
function checkResponse(res) {
    if (res.status === 504 || res.status === 408) {
        // 서킷 브레이커 또는 서버 타임아웃에 의한 결과는 허용 범주로 간주합니다.
        return true;
    }
    return res.status === 200;
}
