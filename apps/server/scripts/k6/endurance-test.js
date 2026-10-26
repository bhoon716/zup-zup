import http from 'k6/http';
import {group, sleep} from 'k6';
import {BASE_URL, getAuthHeaders, validateResponse} from './common-utils.js';





export const options = {
    vus: 500,
    duration: '20m',
    thresholds: {
        'http_req_duration': ['p(95)<300'],
        'http_req_failed': ['rate<0.0001'],
    },
};

export default function () {
    group('지속적인 기본 부하 테스트', function () {

        const res = http.get(`${BASE_URL}/api/v1/courses?page=0&size=20`, getAuthHeaders());


        validateResponse(res, '기본강좌조회');


        sleep(5);
    });
}
