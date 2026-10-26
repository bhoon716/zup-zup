import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {scenario} from 'k6/execution';


export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';


const tokenPool = new SharedArray('JWT Token Pool', function () {
    try {
        return JSON.parse(open('./data/jwts.json'));
    } catch (e) {
        return [];
    }
});






export function validateResponse(res, name = 'API') {
    const success = check(res, {
        [`${name} 상태 코드가 200임`]: (r) => r.status === 200,
        [`${name} 응답 본문이 유효함`]: (r) => r.body.length > 0,
    });
    return success;
}




export function getRandomUserAgent() {
    const agents = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        'Zup-Zup-Bot/1.0',
    ];
    return agents[Math.floor(Math.random() * agents.length)];
}





export function getAuthHeaders(options = {}) {
    const {token = null, isAdmin = false} = options;
    let finalToken = token;


    if (!finalToken && tokenPool.length > 0) {
        if (isAdmin) {
            finalToken = tokenPool[scenario.iterationInTest % 100];
        } else {
            finalToken = tokenPool[scenario.iterationInTest % tokenPool.length];
        }
    }


    if (!finalToken) {
        finalToken = __ENV.AUTH_TOKEN;
    }

    const headers = {
        'Content-Type': 'application/json',
        'User-Agent': getRandomUserAgent(),
    };

    if (finalToken) {
        headers['Authorization'] = `Bearer ${finalToken}`;
    }

    return {headers};
}
