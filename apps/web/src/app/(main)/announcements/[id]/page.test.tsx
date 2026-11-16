import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import AnnouncementDetailPage from "./announcement-detail-page";

const mockUseAnnouncement = vi.fn();
const mockUseParams = vi.fn();

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
        pinned: true,
        createdAt: "2026-07-06T00:00:00.000Z",
        updatedAt: "2026-07-06T00:00:00.000Z",
      },
      isLoading: false,
    });
  });

  it("상단 고정 공지의 제목과 본문을 보여주고 공유 버튼과 중요 표시를 숨긴다", () => {
    render(<AnnouncementDetailPage />);

    expect(screen.getByRole("heading", { name: "공지 제목" })).toBeVisible();
    expect(screen.getByText("본문")).toBeVisible();
    expect(screen.getByRole("link", { name: "공지사항" })).toHaveAttribute("href", "/announcements");
    expect(screen.queryByRole("button", { name: "공지 공유" })).not.toBeInTheDocument();
    expect(screen.queryByText("중요 공지", { exact: true })).not.toBeInTheDocument();
  });
});
