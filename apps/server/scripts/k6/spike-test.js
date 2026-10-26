import http from 'k6/http';
import {group, sleep} from 'k6';
import {BASE_URL, getAuthHeaders, validateResponse} from './common-utils.js';





export const options = {
    stages: [
        {duration: '10s', target: 500},
        {duration: '30s', target: 5000},
        {duration: '1m', target: 5000},
        {duration: '30s', target: 0},
    ],
    thresholds: {
        'http_req_duration': ['p(95)<300'],
        'http_req_failed': ['rate<0.01'],
    },
};

export default function () {
    group('알림 유입 폭주 시뮬레이션', function () {

        const res = http.get(`${BASE_URL}/api/v1/notifications/history`, getAuthHeaders());


        validateResponse(res, '알림히스토리');


        sleep(Math.random() * 1 + 0.5);
    });
}
