



export const IS_LOGGED_IN_COOKIE_NAME = "is_logged_in";





export function deleteCookie(name: string): void {
  if (typeof window !== "undefined") {
    document.cookie = `${name}=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 UTC;`;
  }
}





export function getCookie(name: string): string | undefined {
  if (typeof window === "undefined") return undefined;
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : undefined;
}
