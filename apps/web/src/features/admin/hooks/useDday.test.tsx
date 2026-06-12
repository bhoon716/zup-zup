import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import * as api from "@/shared/api/client";
import { useActiveDday } from "./useDday";

vi.mock("@/shared/api/client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("useActiveDday", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("401은 게스트 상태로 처리하고 refresh를 스킵한다", async () => {
    vi.mocked(api.default.get).mockRejectedValueOnce({
      response: { status: 401 },
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useActiveDday(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
    expect(api.default.get).toHaveBeenCalledWith(
      "/api/v1/ddays/active",
      expect.objectContaining({ skipAuthRefresh: true })
    );
  });
});
