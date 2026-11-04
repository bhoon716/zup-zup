import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Course } from "@/shared/types/api";
import { CourseDetailContent } from "./course-detail-content";

vi.mock("@/shared/ui/kakao-map-embed", () => ({ KakaoMapEmbed: () => <div>지도</div> }));
vi.mock("@/features/review/components/course-review-section", () => ({
  CourseReviewSection: () => <section><p>개인 리뷰 본문</p><textarea aria-label="리뷰 입력" /></section>,
}));

const course: Course = {
  courseKey: "2026:TEST:001:01", subjectCode: "001", classNumber: "01", name: "공개 강의", disclosure: "공개", professor: "담당 교수",
  credits: "3", lectureHours: 3, classTime: "월 6", classroom: "공학관 101", capacity: 20, current: 10, available: 10,
};

describe("강의 상세 Clarity 경계", () => {
  it.each([
    { enabled: undefined, disclosure: "공개" },
    { enabled: false, disclosure: "공개" },
    { enabled: true, disclosure: "비공개" },
    { enabled: true, disclosure: undefined },
  ])("개인 기본값 또는 공개가 아닌 강의는 예외가 없다: $enabled / $disclosure", ({ enabled, disclosure }) => {
    const { container } = render(<CourseDetailContent course={{ ...course, disclosure }} unmaskPublicCourseInfo={enabled} />);
    expect(container.querySelector('[data-clarity-unmask]')).toBeNull();
  });

  it("공개 제목·학점·강의 시간만 허용하고 리뷰·입력·교수·장소는 유지한다", () => {
    const { container } = render(<CourseDetailContent course={course} unmaskPublicCourseInfo />);
    expect(container.querySelectorAll('[data-clarity-unmask="true"]')).toHaveLength(3);
    for (const element of [screen.getByRole("heading", { name: course.name }), screen.getByText("3학점"), screen.getByText("월 6")]) {
      expect(element).toHaveAttribute("data-clarity-unmask", "true");
    }
    for (const element of [screen.getByText("개인 리뷰 본문"), screen.getByRole("textbox", { name: "리뷰 입력" }), screen.getByText("담당 교수"), screen.getByRole("heading", { name: "공학관 101" })]) {
      expect(element.closest("[data-clarity-unmask]")).toBeNull();
    }
  });

  it("정규화한 강의 일정도 텍스트에만 예외를 적용한다", () => {
    render(<CourseDetailContent course={{ ...course, schedules: [{ dayOfWeek: "월", startTime: "09:00", endTime: "10:00" }] }} unmaskPublicCourseInfo />);
    expect(screen.getByText("월 09:00 ~ 10:00")).toHaveAttribute("data-clarity-unmask", "true");
    expect(screen.getByText("시간 및 장소")).not.toHaveAttribute("data-clarity-unmask");
  });
});
