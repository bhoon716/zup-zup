import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { WebPushSection, SettingsActionBar } from "./settings-sections";

describe("WebPushSection Component", () => {
  const defaultProps = {
    deviceAlias: "",
    setDeviceAlias: vi.fn(),
    devices: [
      {
        id: 1,
        type: "WEB" as const,
        alias: "내 크롬 브라우저",
        registeredAt: "2026-07-26T00:00:00Z",
      },
    ],
    loadingWebPush: false,
    watch: vi.fn().mockReturnValue(true),
    setValue: vi.fn(),
    handleRegisterDevice: vi.fn(),
    handleDeleteDevice: vi.fn(),
  };

  it("renders device registration and device list", () => {
    render(<WebPushSection {...defaultProps} />);

    expect(screen.getByText("웹 푸시 알림")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("기기 별칭 (예: 내 노트북)")).toBeInTheDocument();
    expect(screen.getByText("현재 기기 등록")).toBeInTheDocument();
    expect(screen.getByText("내 크롬 브라우저")).toBeInTheDocument();
  });
});

describe("SettingsActionBar Component", () => {
  const defaultProps = {
    isSubmitting: false,
    onCancel: vi.fn(),
  };

  it("renders cancel button and save button", () => {
    render(<SettingsActionBar {...defaultProps} />);

    expect(screen.getByText("변경 취소")).toBeInTheDocument();
    expect(screen.getByText("설정 저장하기")).toBeInTheDocument();
  });

  it("renders test notification button when handler is provided and handles clicks", () => {
    const handleSendTestNotification = vi.fn();
    render(
      <SettingsActionBar
        {...defaultProps}
        handleSendTestNotification={handleSendTestNotification}
      />
    );

    const testBtn = screen.getByText("알림 테스트");
    const separator = screen.getByText("|");
    expect(testBtn).toBeInTheDocument();
    expect(separator).toBeInTheDocument();
    fireEvent.click(testBtn);
    expect(handleSendTestNotification).toHaveBeenCalledTimes(1);
  });

  it("shows cooldown state when testCooldownSeconds > 0", () => {
    const handleSendTestNotification = vi.fn();
    render(
      <SettingsActionBar
        {...defaultProps}
        handleSendTestNotification={handleSendTestNotification}
        testCooldownSeconds={15}
      />
    );

    expect(screen.getByText("테스트 쿨타임 15s")).toBeDisabled();
  });
});
