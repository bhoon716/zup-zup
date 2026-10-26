import http from 'k6/http';
import {group, sleep} from 'k6';
import {BASE_URL, getAuthHeaders, validateResponse} from './common-utils.js';





export const options = {
    vus: 500,
    iterations: 500,
    thresholds: {
        'http_req_duration': ['p(95)<2000'],
    },
};

export default function () {
    group('Start Massive Dispatch Process', function () {

        const payload = JSON.stringify({
            courseKey: '2026:U211600010:ALL_STUDENTS',
            message: '전북대 재학생 여러분, 수강신청 알림 테스트입니다.'
        });


        const res = http.post(`${BASE_URL}/api/v1/admin/courses/crawl`, payload, getAuthHeaders({isAdmin: true}));


        validateResponse(res, 'MassiveDispatchInitiation');


        sleep(5);
    });
}
