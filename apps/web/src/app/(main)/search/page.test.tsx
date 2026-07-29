import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import SearchPage from "./page";
import { COURSE_GUIDE_MODAL_STORAGE_KEY } from "@/features/course/components/course-guide-modal";

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

const { mockAuthState, mockCourseTable, mockUseCourses } = vi.hoisted(() => ({
  mockAuthState: {
    user: null as null | { id: number; email: string; name: string; role: string },
    isLoading: false,
  },
  mockCourseTable: vi.fn(),
  mockUseCourses: vi.fn(),
}));

vi.mock("@/features/course/hooks/useCourses", () => ({
  useCourses: (...args: unknown[]) => mockUseCourses(...args),
  useSearchDefaultSemester: () => ({
    data: { semester: "U211600010" },
    isLoading: false,
  }),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: typeof mockAuthState) => unknown) => selector(mockAuthState),
}));

vi.mock("@/features/course/components/course-search-bar", () => ({
  CourseSearchBar: () => <div data-testid="course-search-bar" />,
}));

vi.mock("@/features/course/components/course-table", () => ({
  CourseTable: (props: unknown) => {
    mockCourseTable(props);
    return <div data-testid="course-table" />;
  },
}));

vi.mock("@/features/course/components/course-table-skeleton", () => ({
  CourseTableSkeleton: () => <div data-testid="course-table-skeleton" />,
}));

describe("SearchPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthState.user = null;
    mockAuthState.isLoading = false;
    Object.defineProperty(window, "localStorage", {
      value: createLocalStorageMock(),
      configurable: true,
    });
    window.localStorage.setItem(COURSE_GUIDE_MODAL_STORAGE_KEY, "seen");
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
    expect(screen.getByRole("button", { name: "사용법" })).toBeInTheDocument();
  });

  it("검색 페이지는 전역 인증 상태의 사용자를 공통 기준으로 사용한다", () => {
    mockAuthState.user = {
      id: 1,
      email: "user@test.com",
      name: "사용자",
      role: "USER",
    };

    render(<SearchPage />);

    expect(mockCourseTable).toHaveBeenCalledWith(
      expect.objectContaining({
        initialUser: mockAuthState.user,
        skipPersonalFetch: false,
      })
    );
  });

  it("모바일 검색어를 로컬에서 편집하고 전체 초기화와 동기화한다", async () => {
    const user = userEvent.setup();
    render(<SearchPage />);

    const getKeywordInput = () => screen.getByPlaceholderText("강의명 또는 학수번호를 입력하세요");
    await user.type(getKeywordInput(), "자료구조");
    await user.click(screen.getByRole("button", { name: "검색" }));

    expect(getKeywordInput()).toHaveValue("자료구조");
    await user.clear(getKeywordInput());
    await user.type(getKeywordInput(), "미적분");
    await user.click(screen.getByRole("button", { name: "초기화" }));

    expect(getKeywordInput()).toHaveValue("");
  });
});
