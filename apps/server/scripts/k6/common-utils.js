export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * 표준 API 응답 검증 (200 OK & JSON)
 */
export function checkResponse(res, scenarioName = 'API') {
  return {
    [`${scenarioName} status is 200`]: (r) => r.status === 200,
    [`${scenarioName} has data`]: (r) => {
      try {
        const body = JSON.parse(r.body);
        return body && body.data !== undefined;
      } catch (e) {
        return false;
      }
    },
  };
}

/**
 * 테스트용 랜덤 유저 에이전트 생성
 */
export function getRandomUserAgent() {
  const agents = [
    'k6-performance-test/1.0',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
  ];
  return agents[Math.floor(Math.random() * agents.length)];
}
