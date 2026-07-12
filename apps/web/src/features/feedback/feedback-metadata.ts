const MAX_OS_LENGTH = 128;
const MAX_LANGUAGE_LENGTH = 35;

type FeedbackMetadata = {
  os?: string;
  language?: string;
};

/**
 * Keeps client-provided feedback metadata small and limited to the server contract.
 */
export function buildFeedbackMetadata(platform: string, language: string): string {
  const metadata: FeedbackMetadata = {};
  const os = platform.trim();
  const normalizedLanguage = language.trim();

  if (os.length > 0 && os.length <= MAX_OS_LENGTH) {
    metadata.os = os;
  }
  if (normalizedLanguage.length > 0 && normalizedLanguage.length <= MAX_LANGUAGE_LENGTH) {
    metadata.language = normalizedLanguage;
  }

  return JSON.stringify(metadata);
}
