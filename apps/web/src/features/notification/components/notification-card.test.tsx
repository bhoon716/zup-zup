import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { NotificationCard } from "./notification-card";
import type { NotificationHistory } from "@/shared/types/api";

describe("NotificationCard", () => {
  it("과거 FCM 이력의 제목과 시간은 보존하고 지원 종료를 표시한다", () => {
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
    expect(screen.getByText("FCM (지원 종료)")).toBeInTheDocument();
    expect(screen.getByText("2024.01.01 12:00")).toBeInTheDocument();
  });
});
