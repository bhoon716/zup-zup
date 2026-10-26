import http from 'k6/http';
import {group} from 'k6';
import {BASE_URL, getAuthHeaders, validateResponse} from './common-utils.js';






export const options = {
    vus: 3000,
    iterations: 3000,
};

export default function () {
    group('동시성 처리 요청', function () {

        const targetCourseKey = '2026:U211600010:0000100017:1';
        const payload = JSON.stringify({courseKey: targetCourseKey});


        const res = http.post(`${BASE_URL}/api/v1/subscriptions`, payload, getAuthHeaders());


        validateResponse(res, '동시성구독신청');
    });
}
