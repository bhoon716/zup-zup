const ALLOWED_EXACT_PATHS = new Set([
  "/",
  "/search",
  "/notifications",
  "/timetable",
]);

const ALLOWED_PATH_PREFIXES = ["/courses/", "/announcements/"];

export const resolveAllowedPwaUrl = (value: string | undefined, origin: string): string => {
  const fallback = new URL("/", origin).href;

  if (!value) {
    return fallback;
  }

  try {
    const url = new URL(value, origin);
    const isSameOrigin = url.origin === origin;
    const isSupportedProtocol = url.protocol === "http:" || url.protocol === "https:";
    const isAllowedPath = ALLOWED_EXACT_PATHS.has(url.pathname)
      || ALLOWED_PATH_PREFIXES.some((prefix) => url.pathname.startsWith(prefix)
        && url.pathname.length > prefix.length);

    return isSameOrigin && isSupportedProtocol && isAllowedPath ? url.href : fallback;
  } catch {
    return fallback;
  }
};
