import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { NavLinks } from "./nav-links";

vi.mock("next/navigation", () => ({
  usePathname: () => "/search",
}));

describe("NavLinks", () => {
  const defaultProps = {
    isAdmin: false,
    isLoggedIn: false,
    onGuardedAction: vi.fn(),
  };

  it("로딩 중(isLoading = true)일 때 스켈레톤 4개를 렌더링한다", () => {
    render(<NavLinks {...defaultProps} isLoading={true} />);
    const skeletons = screen.getAllByTestId("nav-link-skeleton");
    expect(skeletons).toHaveLength(4);
  });

  it("로딩 완료 후 로그인(isLoggedIn = true) 상태이면 로그인 전용 링크들을 노출한다", () => {
    render(<NavLinks {...defaultProps} isLoggedIn={true} isLoading={false} />);
    
    expect(screen.getByText("내 시간표")).toBeInTheDocument();
    expect(screen.getByText("알림 / 구독")).toBeInTheDocument();
    expect(screen.getByText("건의 / 버그")).toBeInTheDocument();
    expect(screen.getByText("설정")).toBeInTheDocument();
  });

  it("로딩 완료 후 비로그인(isLoggedIn = false) 상태이면 로그인 전용 링크들을 숨긴다", () => {
    const { container } = render(<NavLinks {...defaultProps} isLoggedIn={false} isLoading={false} />);
    
    const timetableLink = container.querySelector('a[href="/timetable"]');
    expect(timetableLink).toHaveClass("hidden");
  });

  it("관리자 메뉴는 모바일과 데스크톱에서 알림 DLQ 관리 링크를 제공한다", async () => {
    const { unmount } = render(<NavLinks {...defaultProps} isAdmin isLoggedIn isMobile />);
    expect(screen.getByRole("link", { name: "알림 DLQ 관리" }))
      .toHaveAttribute("href", "/admin/notification-deliveries");

    unmount();
    render(<NavLinks {...defaultProps} isAdmin isLoggedIn />);
    fireEvent.pointerDown(screen.getByRole("button", { name: /관리자/ }));
    expect(await screen.findByRole("menuitem", { name: "알림 DLQ 관리" }))
      .toHaveAttribute("href", "/admin/notification-deliveries");
  });
});
