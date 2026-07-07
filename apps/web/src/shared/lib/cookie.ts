/**
 * 쿠키 제어를 위한 유틸리티 및 전역 상수입니다.
 */

export const IS_LOGGED_IN_COOKIE_NAME = "is_logged_in";

/**
 * 지정된 이름의 브라우저 쿠키를 강제로 삭제(만료) 처리합니다.
 * @param name 삭제할 쿠키명
 */
export function deleteCookie(name: string): void {
  if (typeof window !== "undefined") {
    document.cookie = `${name}=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 UTC;`;
  }
}

/**
 * 브라우저 쿠키에서 지정한 이름의 값을 안전하고 정확하게 파싱하여 반환합니다.
 * @param name 가져올 쿠키명
 */
export function getCookie(name: string): string | undefined {
  if (typeof window === "undefined") return undefined;
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : undefined;
}
