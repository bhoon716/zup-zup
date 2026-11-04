import http from 'k6/http';
import {group, sleep} from 'k6';
import {BASE_URL, getAuthHeaders} from './common-utils.js';





export const options = {
    vus: 2000,
    duration: '3m',
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
    },
};

export default function () {
    group('외부 시스템 의존성 탄력성 테스트', function () {

        const params = getAuthHeaders({isAdmin: true});
        params.timeout = '3s';
        params.tags = {name: '외부의존성체크'};


        const res = http.get(`${BASE_URL}/api/v1/admin/overview`, params);


        checkResponse(res);
        sleep(1);
    });
}




function checkResponse(res) {
    if (res.status === 504 || res.status === 408) {

        return true;
    }
    return res.status === 200;
}
