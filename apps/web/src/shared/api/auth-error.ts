export const isDefinitiveAuthFailure = (error: unknown): boolean => {
  if (!error || typeof error !== "object") {
    return false;
  }

  const status = (error as { response?: { status?: number } }).response?.status;
  return status === 401 || status === 403;
};
