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
        alias: "내 크롬 브라우저",
        maskedToken: "https://example.invalid/...***",
        registeredAt: "2026-07-26T00:00:00Z",
      },
    ],
    loadingWebPush: false,
    webPushEnabled: true,
    setValue: vi.fn(),
    handleRegisterDevice: vi.fn(),
    handleDeleteDevice: vi.fn(),
  };

  it("renders device registration and device list", () => {
    render(<WebPushSection {...defaultProps} />);

    expect(screen.getByText("웹 푸시 알림")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "기기 별칭" })).toBeInTheDocument();
    expect(screen.getByText("현재 기기 등록")).toBeInTheDocument();
    expect(screen.getByText("내 크롬 브라우저")).toBeInTheDocument();
  });

  it.each([
    ["맥북 한 대만 등록한 경우", ["내 맥북"]],
    ["여러 기기를 등록한 경우", ["내 맥북", "내 아이폰"]],
    ["서버 목록 순서가 바뀐 경우", ["내 아이폰", "내 맥북"]],
  ])("%s에도 목록 순서로 현재 기기를 단정하지 않는다", (_, aliases) => {
    render(
      <WebPushSection
        {...defaultProps}
        devices={aliases.map((alias, index) => ({
          ...defaultProps.devices[0], id: index + 1, alias,
        }))}
      />
    );

    expect(screen.queryByText("THIS", { exact: true })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "등록된 기기" })).toBeInTheDocument();
    for (const alias of aliases) {
      expect(screen.getByText(alias, { exact: true })).toBeInTheDocument();
    }
  });

  it("빈 별칭으로 등록하면 입력란을 안내하고 등록을 호출하지 않는다", () => {
    const handleRegisterDevice = vi.fn();
    render(<WebPushSection {...defaultProps} deviceAlias="   " handleRegisterDevice={handleRegisterDevice} />);

    fireEvent.click(screen.getByRole("button", { name: "현재 기기 등록" }));

    expect(screen.getByRole("textbox", { name: "기기 별칭" })).toHaveAccessibleDescription("기기 별칭을 입력해 주세요.");
    expect(screen.getByRole("textbox", { name: "기기 별칭" })).toHaveFocus();
    expect(handleRegisterDevice).not.toHaveBeenCalled();
  });
});

describe("SettingsActionBar Component", () => {
  const defaultProps = {
    isSubmitting: false,
    isDirty: true,
    onCancel: vi.fn(),
  };

  it("renders cancel button and save button", () => {
    render(<SettingsActionBar {...defaultProps} />);

    expect(screen.getByText("변경 취소")).toBeInTheDocument();
    expect(screen.getByText("변경 저장")).toBeInTheDocument();
  });

  it("renders test notification button when handler is provided and handles clicks", () => {
    const handleSendTestNotification = vi.fn();
    render(
      <SettingsActionBar
        {...defaultProps}
        handleSendTestNotification={handleSendTestNotification}
      />
    );

    const testBtn = screen.getByRole("button", { name: "알림 테스트" });
    expect(testBtn).toHaveAccessibleDescription("저장된 설정으로 테스트합니다.");
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

    expect(screen.getByRole("button", { name: "다시 테스트까지 15초" })).toBeDisabled();
  });
});
