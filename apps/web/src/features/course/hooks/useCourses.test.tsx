import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as courseApi from "@/features/course/api/course.api";
import { useCourseDetail, useCourseHistory, useCourses } from "./useCourses";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { CourseSearchCondition } from "@/shared/types/api";

vi.mock("@/features/course/api/course.api", () => ({
  searchCourses: vi.fn(),
  getCourseHistory: vi.fn(),
  getCourseDetail: vi.fn(),
}));

describe("useCourses hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("강의 목록 응답을 화면용 필드로 정규화한다", async () => {
    const mockedSearchCourses = vi.mocked(courseApi.searchCourses);
    mockedSearchCourses.mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
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
      },
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
        code: "SUCCESS",
        message: "ok",
        data: {
          content: [],
          last: false,
          number: 0,
        },
      } as never)
      .mockResolvedValueOnce({
        code: "SUCCESS",
        message: "ok",
        data: {
          content: [],
          last: true,
          number: 1,
        },
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

  it("courseKey가 없으면 강의 이력 조회를 실행하지 않는다", async () => {
    const mockedHistory = vi.mocked(courseApi.getCourseHistory);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCourseHistory(""), { wrapper });

    await waitFor(() => expect(result.current.fetchStatus).toBe("idle"));
    expect(mockedHistory).not.toHaveBeenCalled();
  });

  it("강의 상세 조회 결과를 동일한 규격으로 정규화한다", async () => {
    const mockedDetail = vi.mocked(courseApi.getCourseDetail);
    mockedDetail.mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
        courseKey: "CSE-201",
        subjectCode: "CSE201",
        name: "운영체제",
        classNumber: "02",
        totalSeats: 30,
        currentSeats: 18,
        professorName: "이교수",
      },
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
});
