import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CourseDetailPage from "./course-detail-page";

const { params, detail } = vi.hoisted(() => ({ params: { courseKey: "" }, detail: vi.fn() }));
vi.mock("next/navigation", () => ({ useParams: () => params }));
vi.mock("@/features/course/hooks/useCourses", () => ({ useCourseDetail: (key: string) => detail(key) }));
vi.mock("@/features/course/components/course-detail-content", () => ({ CourseDetailContent: () => <div>강의 정보</div> }));

describe("공개 강의 경로의 키 복원", () => {
  beforeEach(() => {
    detail.mockReset();
    detail.mockReturnValue({ data: null, isLoading: false, error: null });
  });

  it.each(["2026:TERM:COURSE:01", "2026%3ATERM%3ACOURSE%3A01"])("경로 %s를 API 인코딩 전에 한 번 복원한다", (key) => {
    params.courseKey = key;
    render(<CourseDetailPage />);
    expect(detail).toHaveBeenCalledWith("2026:TERM:COURSE:01");
  });

  it("잘못된 percent 인코딩도 조회 실패 안내를 유지한다", () => {
    params.courseKey = "invalid%key";
    render(<CourseDetailPage />);
    expect(detail).toHaveBeenCalledWith("invalid%key");
    expect(screen.getByRole("heading", { name: "강좌를 찾을 수 없습니다" })).toBeInTheDocument();
  });
});
