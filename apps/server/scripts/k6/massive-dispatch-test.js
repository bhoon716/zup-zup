import http from 'k6/http';
import { sleep, group } from 'k6';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Massive Dispatch Test] 대규모 작업 트리거링 및 시스템 영향도 검증
 * 상황: 약 2.5만 명의 유저 데이터가 존재하는 상황에서 관리자가 전체 크롤링/알림 명령을 내릴 때의 부하를 측정합니다.
 */
export const options = {
    vus: 500,        // 500명의 관리자가 동시 다발적으로 명령을 내리는 극한 상황 가정
    iterations: 500,
    thresholds: {
        'http_req_duration': ['p(95)<2000'], // 대규모 작업 요청 응답이 2초 이내에 완료되어야 함
    },
};

export default function () {
    group('Start Massive Dispatch Process', function () {
        // 관리자가 시스템 전체에 영향을 주는 무거운 작업을 지시하는 페이로드입니다.
        const payload = JSON.stringify({
            courseKey: '2026:U211600010:ALL_STUDENTS',
            message: '전북대 재학생 여러분, 수강신청 알림 테스트입니다.'
        });
        
        // 관리자 권한(ROLE_ADMIN) 토큰을 사용하여 백그라운드 작업 시작을 트리거합니다.
        const res = http.post(`${BASE_URL}/api/v1/admin/courses/crawl`, payload, getAuthHeaders({ isAdmin: true }));

        // 요청이 서버 큐에 정상적으로 접수되었는지 확인합니다.
        validateResponse(res, 'MassiveDispatchInitiation');
        
        // 서버의 백그라운드 작업 처리 경과를 관찰하기 위해 5초간 대기합니다.
        sleep(5);
    });
}
