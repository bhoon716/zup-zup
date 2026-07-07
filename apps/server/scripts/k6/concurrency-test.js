import http from 'k6/http';
import { sleep, group } from 'k6';
import { BASE_URL, validateResponse, getAuthHeaders } from './common-utils.js';

/**
 * [Concurrency Test] 데이터 동시성 무결성 검증
 * 상황: 특정 인기 과목에 대해 수천 명의 유저가 동시에 '구독 신청' 버튼을 연타하는 상황을 시뮬레이션합니다.
 * 이 테스트를 통해 데이터베이스 데드락 발생 여부 및 비즈니스 로직의 정합성을 검증합니다.
 */
export const options = {
    vus: 3000,        // 3000명의 유저가 동시 접속
    iterations: 3000, // 총 3000번의 구독 요청 수행
};

export default function () {
    group('동시성 처리 요청', function () {
        // 테스트 대상이 될 특정 강좌 키를 설정합니다.
        const targetCourseKey = '2026:U211600010:0000100017:1';
        const payload = JSON.stringify({ courseKey: targetCourseKey });
        
        // 인증 풀에서 각 VU별로 할당된 토큰을 사용하여 구독 API를 호출합니다.
        const res = http.post(`${BASE_URL}/api/v1/subscriptions`, payload, getAuthHeaders());

        // 구독 신청 결과가 성공(200 OK)인지 확인합니다.
        validateResponse(res, '동시성구독신청');
    });
}
