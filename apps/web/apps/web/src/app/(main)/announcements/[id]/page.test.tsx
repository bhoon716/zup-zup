import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import AnnouncementDetailPage from "./page";

const mockUseAnnouncement = vi.fn();
const mockUseParams = vi.fn();
const shareSpy = vi.fn();

vi.mock("next/navigation", () => ({
  useParams: () => mockUseParams(),
}));

vi.mock("@/features/announcement/hooks/useAnnouncements", () => ({
  useAnnouncement: (id: number) => mockUseAnnouncement(id),
}));

describe("AnnouncementDetailPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({ id: "1" });
    mockUseAnnouncement.mockReturnValue({
      data: {
        id: 1,
        title: "공지 제목",
        content: "본문",
        pinned: false,
        createdAt: "2026-07-06T00:00:00.000Z",
        updatedAt: "2026-07-06T00:00:00.000Z",
      },
      isLoading: false,
    });
    Object.defineProperty(window.navigator, "share", {
      configurable: true,
      value: shareSpy,
    });
    window.history.pushState({}, "", "/announcements/1");
  });

  it("공지 공유 버튼에 접근 가능한 이름을 제공하고 공유 API를 호출한다", () => {
    render(<AnnouncementDetailPage />);

    const shareButton = screen.getByRole("button", { name: "공지 공유" });
    fireEvent.click(shareButton);

    expect(shareSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "공지 제목",
        url: "http://localhost:3000/announcements/1",
      })
    );
  });
});
