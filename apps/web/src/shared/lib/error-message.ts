export function localizedErrorMessage(message: unknown, fallback: string): string {
  if (typeof message === "string" && /\p{Script=Hangul}/u.test(message)) {
    return message;
  }
  return fallback;
}
