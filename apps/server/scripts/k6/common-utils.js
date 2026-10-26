import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {scenario} from 'k6/execution';


export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';


const sessions = new SharedArray('Session Pool', function () {
    try {
        return JSON.parse(open('./data/sessions.json'));
    } catch {
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
    const {sessionCookie = null, csrfToken = null, isAdmin = false} = options;
    const candidates = sessions.filter(session => Boolean(session.admin) === isAdmin);
    const session = candidates.length > 0 ? candidates[scenario.iterationInTest % candidates.length] : null;
    const cookie = sessionCookie || session?.cookie || __ENV.SESSION_COOKIE;
    const csrf = csrfToken || session?.csrfToken || __ENV.CSRF_TOKEN;
    const headers = {
        'Content-Type': 'application/json',
        'User-Agent': getRandomUserAgent(),
    };
    if (cookie) {
        headers.Cookie = cookie;
    }
    if (csrf) {
        headers['X-CSRF-TOKEN'] = csrf;
    }
    return {headers};
}
