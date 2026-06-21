import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { NotificationCard } from "./notification-card";
import type { NotificationHistory } from "@/shared/types/api";

describe("NotificationCard", () => {
  it("서버가 준 제목, 본문, 채널, 시간을 그대로 렌더링한다", () => {
    const notification: NotificationHistory = {
      id: 1,
      courseKey: "CSE-101",
      title: "공석 발생",
      message: "[자료구조] 과목에 공석이 발생했습니다.\n현재 3자리 남았습니다.",
      channel: "FCM",
      sentAt: "2024-01-01T12:00:00",
      createdAt: "2024-01-01T12:00:00",
    };

    render(<NotificationCard notification={notification} />);

    expect(screen.getByText("공석 발생")).toBeInTheDocument();
    expect(
      screen.getByText((_, element) => element?.textContent === "[자료구조] 과목에 공석이 발생했습니다.\n현재 3자리 남았습니다.")
    ).toBeInTheDocument();
    expect(screen.getByText("FCM")).toBeInTheDocument();
    expect(screen.getByText("2024.01.01 12:00")).toBeInTheDocument();
  });
});
