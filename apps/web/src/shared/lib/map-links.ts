const ONLINE_KEYWORDS = ['온라인', '원격', '비대면', 'zoom', '줌'];

const ROOM_TOKEN_PATTERN =
  /^(?:[A-Za-z]?\d{1,4}(?:-\d{1,4})?|[A-Za-z가-힣]*\d+호(?:관|실)?|[A-Za-z가-힣]*\d+실|[A-Za-z가-힣]*\d+관|\d+호|\d+실|\d+관|\d{1,4})$/;

function normalizeClassroomText(value: string): string {
  return value
    .replace(/\([^)]*\)/g, ' ')
    .replace(/[:]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function stripRoomToken(value: string): string {
  let cleaned = value;
  if (/-\d{1,4}(?:호|관|실)?$/i.test(cleaned)) {
    cleaned = cleaned.replace(/-\d{1,4}(?:호|관|실)?$/i, '').trim();
  }

  const tokens = cleaned.split(' ').filter(Boolean);
  if (tokens.length <= 1) {
    return cleaned;
  }

  const lastToken = tokens[tokens.length - 1];
  if (ROOM_TOKEN_PATTERN.test(lastToken)) {
    tokens.pop();
  }

  return tokens.join(' ').trim();
}

export function getCampusMapQuery(classroom?: string): string | null {
  if (!classroom) {
    return null;
  }

  const normalized = normalizeClassroomText(classroom);
  if (!normalized) {
    return null;
  }

  const lower = normalized.toLowerCase();
  if (ONLINE_KEYWORDS.some((keyword) => lower.includes(keyword.toLowerCase()))) {
    return null;
  }

  const building = stripRoomToken(normalized);
  if (!building) {
    return null;
  }

  if (building.includes('전북대학교')) {
    return building;
  }

  return `전북대학교 ${building}`.trim();
}

export function getMapSearchUrls(classroom?: string): { kakao: string; naver: string } | null {
  const query = getCampusMapQuery(classroom);
  if (!query) {
    return null;
  }

  const encoded = encodeURIComponent(query);
  return {
    kakao: `https://map.kakao.com/?q=${encoded}`,
    naver: `https://map.naver.com/p/search/${encoded}`,
  };
}
