import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { scenario } from 'k6/execution';

// 성능 테스트의 기본 URL 설정 (환경변수 또는 로컬)
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 사전 생성된 JWT 토큰 파일에서 데이터를 로드하여 공유 배열을 생성합니다.
const tokenPool = new SharedArray('JWT Token Pool', function () {
    try {
        return JSON.parse(open('./data/jwts.json'));
    } catch (e) {
        return [];
    }
});

/**
 * HTTP 응답의 상태 코드와 본문 존재 여부를 검증합니다.
 * @param {Response} res k6 응답 객체
 * @param {string} name 검증 대상의 식별 이름
 */
export function validateResponse(res, name = 'API') {
    const success = check(res, {
        [`${name} 상태 코드가 200임`]: (r) => r.status === 200,
        [`${name} 응답 본문이 유효함`]: (r) => r.body.length > 0,
    });
    return success;
}

/**
 * 다양한 브라우저 환경을 시뮬레이션하기 위해 랜덤한 User-Agent 문자열을 반환합니다.
 */
export function getRandomUserAgent() {
    const agents = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        'JBNU-Sugang-Helper-Bot/1.0',
    ];
    return agents[Math.floor(Math.random() * agents.length)];
}

/**
 * 시나리오의 실행 인덱스에 따라 토큰 풀에서 적절한 JWT를 선택하여 인증 헤더를 구성합니다.
 * @param {Object} options token 직접 지정 또는 isAdmin 여부 설정
 */
export function getAuthHeaders(options = {}) {
    const { token = null, isAdmin = false } = options;
    let finalToken = token;
    
    // 토큰이 직접 전달되지 않은 경우, 로드된 토큰 풀에서 권한에 맞춰 순차적으로 할당합니다.
    if (!finalToken && tokenPool.length > 0) {
        if (isAdmin) {
            finalToken = tokenPool[scenario.iterationInTest % 100];
        } else {
            finalToken = tokenPool[scenario.iterationInTest % tokenPool.length];
        }
    }
    
    // 풀에도 토큰이 없는 경우 환경 변수(AUTH_TOKEN)를 최종 폴백으로 사용합니다.
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

    return { headers };
}
