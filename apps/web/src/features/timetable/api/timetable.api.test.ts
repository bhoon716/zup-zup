import { beforeEach, expect, it, vi } from "vitest";
import api from "@/shared/api/client";
import { timetableApi } from "./timetable.api";

vi.mock("@/shared/api/client", () => ({ default: { get: vi.fn() } }));

beforeEach(() => vi.clearAllMocks());

it.each(["", null])("대표 시간표가 없는 직접 응답 %s는 null로 해석한다", async (data) => {
  vi.mocked(api.get).mockResolvedValue({ data, status: 200 });
  expect(await timetableApi.getPrimaryTimetable()).toBeNull();
  expect(api.get).toHaveBeenCalledWith("/api/v1/timetables/primary");
});
