import http from 'k6/http';
import {group, sleep} from 'k6';
import {SharedArray} from 'k6/data';
import {BASE_URL, getAuthHeaders, validateResponse} from './common-utils.js';





const filters = new SharedArray('검색 필터 데이터', function () {
    return [
        {query: '사회적가치와개인의자유', academicYear: '2026'},
        {query: '고사와성어의탐구', semester: '1'},
        {generalCategory: '일반선택', credits: 2},
        {department: '간호학', targetGrade: 1},
        {professor: '외부교원1'},
        {query: 'AI를활용한음악만들기', professor: '외부교원1'},
        {department: '전체(학부)', academicYear: '2026'},
        {query: '글로벌시대의세계시민'}
    ];
});

export const options = {
    stages: [
        {duration: '30s', target: 500},
        {duration: '2m', target: 1500},
        {duration: '1m', target: 1500},
        {duration: '30s', target: 0},
    ],
    thresholds: {
        'http_req_duration': ['p(99)<1000'],
    },
};

export default function () {
    group('다양한 필터 검색 테스트', function () {

        const filter = filters[Math.floor(Math.random() * filters.length)];


        const queryParams = Object.keys(filter)
            .map(key => `${key}=${encodeURIComponent(filter[key])}`)
            .join('&');


        const res = http.get(`${BASE_URL}/api/v1/courses?${queryParams}`, getAuthHeaders());


        validateResponse(res, '필터검색');


        sleep(Math.random() * 0.2 + 0.1);
    });
}
