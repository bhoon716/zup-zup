import type { ComponentProps, FocusEventHandler, MouseEventHandler, ReactNode } from "react";
import { Bell, Clock3, MapPin, Star, UserRound } from "lucide-react";

import { cn } from "@/shared/lib/utils";
import { formatClassification } from "@/shared/lib/formatters";
import type { NormalizedCourse } from "@/shared/lib/course";
import { getCourseSchedule } from "@/shared/lib/course-schedule";
import { Button } from "@/shared/ui/button";

export const courseResultActionSize = "h-7 w-7 rounded-lg md:h-8 md:w-8";
export const courseResultActionBorder = "border-gray-200 text-gray-500";

export function CourseSeatAlertButton({ subscribed, ...buttonProps }: {
  subscribed: boolean;
} & Pick<ComponentProps<typeof Button>, "onClick" | "disabled" | "aria-label" | "title" | "tabIndex">) {
  return (
    <Button
      type="button"
      variant={subscribed ? "default" : "outline"}
      size="icon"
      className={cn(courseResultActionSize, subscribed ? "bg-primary text-white hover:bg-primary/90" : courseResultActionBorder)}
      {...buttonProps}
    >
      <Bell className={cn("h-4 w-4", subscribed && "fill-white")} />
    </Button>
  );
}

function getSeatStatus(available: number) {
  if (available <= 0) {
    return {
      label: "마감됨",
      badgeClass: "bg-red-100 text-red-800",
      barClass: "bg-red-500",
    };
  }

  if (available <= 5) {
    return {
      label: "마감 임박",
      badgeClass: "bg-amber-100 text-amber-800",
      barClass: "bg-amber-500",
    };
  }

  return {
    label: "빈자리 있음",
    badgeClass: "bg-emerald-100 text-emerald-800",
    barClass: "bg-emerald-500",
  };
}

function getClassificationStripe(classification?: string) {
  const normalized = classification || "";

  if (normalized.includes("전공필수")) {
    return "bg-blue-500";
  }

  if (normalized.includes("전공")) {
    return "bg-purple-500";
  }

  if (normalized.includes("교양")) {
    return "bg-emerald-500";
  }

  if (normalized.includes("교직")) {
    return "bg-amber-500";
  }

  return "bg-gray-400";
}

export function CourseResultCard({ course, unmaskPublicCourseInfo, onFocus, onOpen, actions }: {
  course: NormalizedCourse;
  unmaskPublicCourseInfo?: boolean;
  onFocus?: FocusEventHandler<HTMLElement>;
  onOpen?: MouseEventHandler<HTMLButtonElement>;
  actions: ReactNode;
}) {
  const { blocks, fallback } = getCourseSchedule(course);
  const clarityUnmask = unmaskPublicCourseInfo && course.disclosure === "공개" ? true : undefined;
  const { capacity, current, available } = course;
  const seatRatio = capacity > 0 ? Math.min((current / capacity) * 100, 100) : 0;
  const seatStatus = getSeatStatus(available);

  return (
    <article
      className="group relative overflow-hidden rounded-xl border border-border bg-white transition-all duration-300 hover:border-primary/40 hover:shadow-md"
      onFocus={onFocus}
    >
      <div className="flex items-stretch">
        <div
          className={cn(
            "w-1 shrink-0 md:w-1.5",
            getClassificationStripe(course.classification),
          )}
        />

        <div className="flex min-w-0 flex-1 flex-col p-3 md:flex-row md:items-center md:gap-4 md:p-4">
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0 flex-1">
                <div className="mb-1 flex flex-wrap items-center gap-1 md:mb-1.5 md:gap-1.5">
                  <span className="rounded border border-primary/20 bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
                    {formatClassification(course.classification)}
                  </span>
                  <span data-clarity-unmask={clarityUnmask} className="rounded border border-gray-200 bg-gray-100 px-1.5 py-0.5 text-[10px] font-bold text-gray-600">
                    {course.credits || 0}학점
                  </span>
                  <span className="text-[10px] font-semibold tracking-wide text-gray-600">
                    {course.stdtrNo || "학수번호 미제공"}
                  </span>
                </div>

                <h2 data-clarity-unmask={clarityUnmask} className="mb-1 text-sm font-bold text-foreground md:mb-1.5 md:text-base">
                  {onOpen ? (
                    <button
                      type="button"
                      className="block max-w-full truncate rounded-sm text-left transition-colors after:absolute after:inset-0 after:content-[''] group-hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                      onClick={onOpen}
                    >
                      {course.name}
                    </button>
                  ) : course.name}
                </h2>
              </div>

              <div className="flex items-center gap-2 pt-1 md:hidden">
                <span
                  className={cn(
                    "rounded-sm px-1.5 py-0.5 text-[9px] font-bold shrink-0",
                    seatStatus.badgeClass,
                  )}
                >
                  {seatStatus.label}
                </span>
                <div className="flex flex-col items-end gap-0.5">
                  <span className="text-[10px] font-bold text-gray-900">
                    {current} / {capacity}
                  </span>
                  <div className="h-1 w-12 overflow-hidden rounded-full bg-gray-100">
                    <div
                      className={cn("h-full transition-all", seatStatus.barClass)}
                      style={{ width: `${seatRatio}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-gray-600 md:text-[11px]">
              <span className="flex items-center gap-1">
                <UserRound className="h-3 w-3 text-gray-400 md:h-3.5 md:w-3.5" />
                <span className="font-medium text-gray-700">
                  {course.professor || "미지정"}
                </span>
              </span>

              <span className="flex min-w-0 items-baseline gap-1">
                <Clock3 aria-hidden="true" className="relative top-0.5 h-3 w-3 shrink-0 self-start text-gray-400 md:h-3.5 md:w-3.5" />
                <span data-clarity-unmask={clarityUnmask} className="tabular-nums">
                  {blocks.length > 0 ? blocks.map(({ label }) => label).join(", ") : fallback}
                </span>
              </span>

              <span className="flex items-center gap-1">
                <MapPin className="h-3 w-3 text-gray-400 md:h-3.5 md:w-3.5" />
                <span className="max-w-[100px] truncate md:max-w-none">
                  {course.classroom || "강의실 미배정"}
                </span>
              </span>
            </div>
          </div>

          <div className="mt-3 flex items-center justify-between border-t border-border pt-2.5 md:mt-0 md:flex md:flex-col md:justify-center md:min-w-[190px] md:border-l md:border-t-0 md:pl-6 md:pt-0">
            <div className="hidden w-full md:block">
              <div className="mb-2.5 flex items-center justify-between gap-2">
                <span
                  className={cn(
                    "rounded px-1.5 py-0.5 text-[10px] font-bold",
                    seatStatus.badgeClass,
                  )}
                >
                  {seatStatus.label}
                </span>
                <span className="text-xs font-bold text-gray-900">
                  {current} / {capacity}
                </span>
              </div>

              <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
                <div
                  className={cn("h-full rounded-full transition-all", seatStatus.barClass)}
                  style={{ width: `${seatRatio}%` }}
                />
              </div>
            </div>

            <div
              className="relative z-10 mt-0 flex w-full items-center justify-between gap-1 md:mt-4 md:w-full md:gap-1.5"
            >
              <div className="flex items-center gap-1">
                <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
                <span className="text-xs font-bold text-gray-700">{course.averageRating ? course.averageRating.toFixed(1) : "0.0"}</span>
                <span className="text-[10px] text-gray-600">({course.reviewCount || 0})</span>
              </div>

              <div className="flex items-center gap-1 md:gap-1.5">
                {actions}
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}
