export const isUnauthenticated = (error: unknown): boolean => {
  if (!error || typeof error !== "object") {
    return false;
  }

  const status = (error as { response?: { status?: number } }).response?.status;
  return status === 401;
};

export const isCsrfFailure = (error: unknown): boolean => {
  if (!error || typeof error !== "object") return false;
  const response = (error as { response?: { status?: number; data?: { code?: unknown } } }).response;
  return response?.status === 403 && response.data?.code === "A003";
};
