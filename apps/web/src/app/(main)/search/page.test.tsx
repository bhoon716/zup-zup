import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import SearchPage from "./page";

const { mockSetUser, mockUseCourses } = vi.hoisted(() => ({
  mockSetUser: vi.fn(),
  mockUseCourses: vi.fn(),
}));

vi.mock("@/features/course/hooks/useCourses", () => ({
  useCourses: (...args: unknown[]) => mockUseCourses(...args),
  useSearchDefaultSemester: () => ({
    data: { semester: "U211600010" },
    isLoading: false,
  }),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: () => ({
    data: null,
    isLoading: false,
  }),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setUser: (user: null) => void }) => unknown) =>
    selector({
      setUser: mockSetUser,
    }),
}));

vi.mock("@/features/course/components/course-search-bar", () => ({
  CourseSearchBar: () => <div data-testid="course-search-bar" />,
}));

vi.mock("@/features/course/components/course-table", () => ({
  CourseTable: () => <div data-testid="course-table" />,
}));

vi.mock("@/features/course/components/course-table-skeleton", () => ({
  CourseTableSkeleton: () => <div data-testid="course-table-skeleton" />,
}));

describe("SearchPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseCourses.mockReturnValue({
      data: { pages: [{ content: [] }] },
      isLoading: false,
      error: null,
      fetchNextPage: vi.fn(),
      hasNextPage: false,
      isFetchingNextPage: false,
    });
  });

  it("정렬 기준과 정렬 방향 버튼에 이름을 제공한다", () => {
    render(<SearchPage />);

    expect(screen.getByRole("combobox", { name: "정렬 기준" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "정렬 방향 오름차순" })).toBeInTheDocument();
  });
});
