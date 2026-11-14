import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as courseApi from "@/features/course/api/course.api";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseDetail, useCourses, useSearchDefaultSemester } from "./useCourses";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { Course, CourseSearchCondition, CourseSearchPageResponse } from "@/shared/types/api";

vi.mock("@/features/course/api/course.api", () => ({
  searchCourses: vi.fn(),
  getCourseDetail: vi.fn(),
  getSearchDefaultSemester: vi.fn(),
}));

describe("useCourses hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().logout();
  });

  it("강의 목록 응답을 화면용 필드로 정규화한다", async () => {
    const mockedSearchCourses = vi.mocked(courseApi.searchCourses);
    mockedSearchCourses.mockResolvedValue({
        content: [
          {
            courseKey: "CSE-101",
            subjectCode: "CSE101",
            name: "자료구조",
            classNumber: "01",
            totalSeats: 40,
            currentSeats: 33,
            professorName: "김교수",
          },
        ],
        last: true,
        number: 0,
      } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const condition: CourseSearchCondition = { name: "자료" };
    const { result } = renderHook(() => useCourses(condition), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const normalizedCourse = result.current.data?.pages[0].content[0];
    expect(normalizedCourse?.capacity).toBe(40);
    expect(normalizedCourse?.current).toBe(33);
    expect(normalizedCourse?.available).toBe(7);
    expect(normalizedCourse?.professor).toBe("김교수");
  });

  it("무한 스크롤 다음 페이지 요청 시 page 값을 증가시킨다", async () => {
    const mockedSearchCourses = vi.mocked(courseApi.searchCourses);
    mockedSearchCourses
      .mockResolvedValueOnce({
          content: [],
          last: false,
          number: 0,
        } as never)
      .mockResolvedValueOnce({
          content: [],
          last: true,
          number: 1,
        } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const condition: CourseSearchCondition = { name: "알고리즘" };
    const { result } = renderHook(() => useCourses(condition), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    await act(async () => {
      await result.current.fetchNextPage();
    });

    expect(mockedSearchCourses).toHaveBeenNthCalledWith(1, condition, 0);
    expect(mockedSearchCourses).toHaveBeenNthCalledWith(2, condition, 1);
  });

  it("같은 공개 검색에 바로 돌아오면 기존 결과를 재사용한다", async () => {
    const sort = { empty: true, sorted: false, unsorted: true };
    vi.mocked(courseApi.searchCourses).mockResolvedValue({
      content: [], last: true, first: true, number: 0, size: 30, numberOfElements: 0, empty: true, sort,
      pageable: { pageNumber: 0, pageSize: 30, sort, offset: 0, paged: true, unpaged: false },
    });
    const wrapper = createQueryWrapper(createTestQueryClient());
    const condition = { name: "자료" };
    const first = renderHook(() => useCourses(condition), { wrapper });
    await waitFor(() => expect(first.result.current.isSuccess).toBe(true));
    first.unmount();

    const second = renderHook(() => useCourses({ ...condition }), { wrapper });
    expect(second.result.current.isSuccess).toBe(true);
    expect(courseApi.searchCourses).toHaveBeenCalledTimes(1);
  });

  it("공개 강의 초기값을 사용해도 인증 후 개인 리뷰 여부를 새로 조회한다", async () => {
    const course: Course = { courseKey: "CSE-201", subjectCode: "CSE201", classNumber: "02", name: "운영체제" };
    vi.mocked(courseApi.getCourseDetail).mockResolvedValue({ ...course, isReviewed: true });
    const client = createTestQueryClient();
    client.setDefaultOptions({ queries: { retry: false, staleTime: 60_000 } });
    const { result } = renderHook(() => useCourseDetail(course.courseKey, { data: course, updatedAt: Date.now() }), {
      wrapper: createQueryWrapper(client),
    });
    expect(result.current.data?.isReviewed).toBe(false);
    expect(courseApi.getCourseDetail).not.toHaveBeenCalled();

    act(() => useAuthStore.getState().setUser({
      id: 7, name: "사용자", email: "course@example.invalid", role: "USER", onboardingCompleted: true,
      emailEnabled: true, webPushEnabled: false, discordEnabled: false,
    }));

    await waitFor(() => expect(result.current.data?.isReviewed).toBe(true));
    expect(courseApi.getCourseDetail).toHaveBeenCalledExactlyOnceWith(course.courseKey);
    expect(client.getQueryData<Course>(["public-course-detail", course.courseKey])?.isReviewed).toBe(false);
  });

  it("강의 상세 조회 결과를 동일한 규격으로 정규화한다", async () => {
    const mockedDetail = vi.mocked(courseApi.getCourseDetail);
    mockedDetail.mockResolvedValue({
        courseKey: "CSE-201",
        subjectCode: "CSE201",
        name: "운영체제",
        classNumber: "02",
        totalSeats: 30,
        currentSeats: 18,
        professorName: "이교수",
      } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCourseDetail("CSE-201"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.capacity).toBe(30);
    expect(result.current.data?.current).toBe(18);
    expect(result.current.data?.available).toBe(12);
    expect(result.current.data?.professor).toBe("이교수");
  });

  it("검색 기본 학기 조회 결과를 반환한다", async () => {
    const mockedDefaultSemester = vi.mocked(courseApi.getSearchDefaultSemester);
    mockedDefaultSemester.mockResolvedValue({ semester: "U211600025" } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useSearchDefaultSemester(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ semester: "U211600025" });
  });

  it("초기 페이지가 있으면 첫 검색 요청을 다시 보내지 않고 정규화를 적용한다", async () => {
    const mockedSearchCourses = vi.mocked(courseApi.searchCourses);
    const initialPage = {
      content: [
        {
          courseKey: "CSE-401",
          subjectCode: "CSE401",
          name: "분산시스템",
          classNumber: "01",
          totalSeats: 30,
          currentSeats: 10,
          professorName: "이교수",
        },
      ],
      last: true,
      number: 0,
    } as unknown as CourseSearchPageResponse;

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const condition: CourseSearchCondition = { name: "분산" };
    const { result } = renderHook(() => useCourses(condition, { initialPage }), { wrapper });

    await waitFor(() => expect(mockedSearchCourses).not.toHaveBeenCalled());
    const firstCourse = result.current.data?.pages[0].content[0];
    expect(firstCourse?.capacity).toBe(30);
    expect(firstCourse?.current).toBe(10);
    expect(firstCourse?.available).toBe(20);
    expect(firstCourse?.professor).toBe("이교수");
  });
});
