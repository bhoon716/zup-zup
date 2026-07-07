import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { COURSE_GUIDE_MODAL_STORAGE_KEY, CourseGuideModal } from "./course-guide-modal";

const createLocalStorageMock = () => {
  const store = new Map<string, string>();

  return {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => {
      store.clear();
    },
  };
};

describe("CourseGuideModal", () => {
  beforeEach(() => {
    Object.defineProperty(window, "localStorage", {
      value: createLocalStorageMock(),
      configurable: true,
    });
  });

  afterEach(() => {
    Object.defineProperty(window, "localStorage", {
      value: createLocalStorageMock(),
      configurable: true,
    });
  });

  it("처음 진입하면 가이드 모달을 자동으로 연다", async () => {
    render(<CourseGuideModal />);

    expect(await screen.findByText("강의 검색 사용법")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "이해했어요" })).toBeInTheDocument();
  });

  it("닫으면 저장하고 다시 노출하지 않는다", async () => {
    const { rerender } = render(<CourseGuideModal />);

    fireEvent.click(await screen.findByRole("button", { name: "이해했어요" }));

    await waitFor(() => {
      expect(screen.queryByText("강의 검색 사용법")).not.toBeInTheDocument();
    });

    expect(window.localStorage.getItem(COURSE_GUIDE_MODAL_STORAGE_KEY)).toBe("seen");

    rerender(<CourseGuideModal />);

    expect(screen.getByRole("button", { name: "사용법" })).toBeInTheDocument();
    expect(screen.queryByText("강의 검색 사용법")).not.toBeInTheDocument();
  });
});
