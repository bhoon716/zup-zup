"use client";

import { useState } from "react";
import { AlertCircle, ChevronDown, Clock3, ExternalLink, MapPin, UserRound, Users } from "lucide-react";
import type { Course } from "@/shared/types/api";
import { formatClassification, formatGradingMethod, formatLanguage, formatRelativeTime, formatTargetGrade } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { getReviewScopeKey, hasIdentifiableProfessor, normalizeCourse, type NormalizedCourse } from "@/shared/lib/course";
import { getCourseSchedule } from "@/shared/lib/course-schedule";
import { getCampusMapQuery } from "@/shared/lib/map-links";
import { KakaoMapEmbed } from "@/shared/ui/kakao-map-embed";
import { CourseReviewSection } from "@/features/review/components/course-review-section";

interface CourseDetailContentProps {
  course: Course;
  unmaskPublicCourseInfo?: boolean;
}

export function CourseDetailContent({ course: rawCourse, unmaskPublicCourseInfo = false }: CourseDetailContentProps) {
  const course = normalizeCourse(rawCourse);
  const clarityUnmask = unmaskPublicCourseInfo && course.disclosure === "공개" ? true : undefined;
  const targetGradeLabel = formatTargetGrade(course.targetGrade);
  const departmentLabel = formatDepartmentLabel(course.department, targetGradeLabel);
  const isFull = course.capacity > 0 && course.available <= 0;

  return (
    <div className="space-y-6 bg-white px-4 py-6 sm:px-6 md:px-8 dark:bg-[#121212]">
      <header className="pr-8">
        <div className="flex flex-wrap items-center gap-2.5">
          <span className="rounded-full border border-gray-200 bg-gray-100 px-3 py-1 text-xs font-medium tracking-tight wrap-anywhere text-gray-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300">
            {course.stdtrNo || "학수번호 미제공"}
          </span>
          {isFull && (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-red-100 bg-red-50 px-3 py-1 text-xs font-bold text-red-600 dark:border-red-900/30 dark:bg-red-900/20 dark:text-red-400">
              <span className="size-1.5 rounded-full bg-red-500" aria-hidden="true" />
              마감됨
            </span>
          )}
          {!course.isSubscribable && (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-gray-200 bg-gray-50 px-3 py-1 text-xs font-bold text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
              <AlertCircle className="size-3 shrink-0" aria-hidden="true" />
              조회 전용 (종료된 학기)
            </span>
          )}
        </div>
        <h1 data-clarity-unmask={clarityUnmask} className="relative mt-4 pl-3 text-3xl leading-tight font-black tracking-tight wrap-anywhere text-gray-900 before:absolute before:inset-y-1 before:left-0 before:w-1 before:rounded-full before:bg-primary/70 md:text-4xl dark:text-gray-100">
          {course.name}
        </h1>
        <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-gray-900 dark:text-gray-100">
          <span className="inline-flex min-w-0 items-center gap-1.5 rounded-full border border-gray-100 bg-gray-50 px-3 py-1 font-bold dark:border-gray-800 dark:bg-gray-800/50">
            <UserRound className="size-4 shrink-0 text-primary" aria-hidden="true" />
            <span className="wrap-anywhere">{course.professor}</span>
          </span>
          {departmentLabel && (
            <span className="font-medium wrap-anywhere">{departmentLabel} {targetGradeLabel === "전체" ? "" : targetGradeLabel}</span>
          )}
          {course.classification && (
            <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-bold text-primary dark:text-primary-light">
              {formatClassification(course.classification)}
            </span>
          )}
          {course.disclosure && (
            <span className={cn(
              "rounded px-2 py-0.5 text-xs font-bold wrap-anywhere",
              course.disclosure === "공개"
                ? "bg-green-50 text-green-600 dark:bg-green-900/20 dark:text-green-400"
                : "bg-amber-50 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400"
            )}>
              {course.disclosure}
              {course.disclosure === "비공개" && course.disclosureReason && (
                <span className="ml-1 font-medium opacity-70">({course.disclosureReason})</span>
              )}
            </span>
          )}
        </div>
      </header>

      <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <CourseSummaryCard label="학점 / 시간" value={`${course.credits || "-"}학점`} clarityUnmask={clarityUnmask} lectureHours={course.lectureHours} />
        {[
          ["성적평가", formatGradingMethod(course.gradingMethod)],
          ["강의방식", course.courseStatus || "일반"],
          ["강의언어", formatLanguage(course.lectureLanguage)],
        ].map(([label, value]) => (
          <CourseSummaryCard key={label} label={label} value={value} />
        ))}
      </dl>

      <div className="grid gap-6 lg:grid-cols-3">
        <CourseTimeLocation course={course} clarityUnmask={clarityUnmask} />
        <CourseSeatStatus course={course} />
      </div>

      <CourseReviewSection
        courseKey={course.courseKey}
        reviewScopeKey={getReviewScopeKey(rawCourse)}
        averageRating={course.averageRating}
        reviewCount={course.reviewCount}
        isReviewed={course.isReviewed}
        isProfessorUnassigned={!hasIdentifiableProfessor(rawCourse)}
      />
    </div>
  );
}

function CourseSummaryCard({ label, value, lectureHours, clarityUnmask }: {
  label: string;
  value: string;
  lectureHours?: number;
  clarityUnmask?: true;
}) {
  return (
    <div className="group flex min-w-0 flex-col items-center justify-center gap-1 rounded-2xl border border-gray-100 bg-gray-50 p-4 text-center transition-colors hover:border-primary/20 hover:bg-primary/5 dark:border-white/10 dark:bg-white/5">
      <dt className="mb-1 text-xs font-semibold tracking-wider text-gray-500 dark:text-gray-400">{label}</dt>
      <dd data-clarity-unmask={clarityUnmask} className="text-lg font-bold wrap-anywhere text-gray-900 transition-colors group-hover:text-primary dark:text-gray-100">
        {value}
        {lectureHours ? <span className="text-sm font-normal text-gray-500 dark:text-gray-400"> ({lectureHours}시간)</span> : null}
      </dd>
    </div>
  );
}

function CourseTimeLocation({ course, clarityUnmask }: { course: Course; clarityUnmask?: true }) {
  const { blocks, fallback } = getCourseSchedule(course);
  const mapQuery = getCampusMapQuery(course.classroom);
  const [showMap, setShowMap] = useState(false);

  return (
    <section className="flex min-w-0 flex-col gap-3 lg:col-span-2">
      <h2 className="flex items-center gap-2 text-base font-bold text-gray-900 dark:text-gray-100">
        <Clock3 className="size-5 text-primary" aria-hidden="true" />
        시간 및 장소
      </h2>

      <div className="grid flex-1 gap-y-6 rounded-3xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6 md:grid-cols-2 dark:border-gray-700 dark:bg-[#1E1E1E]">
        <div className="min-w-0 space-y-4 md:pr-8">
          <h3 className="text-xs font-bold tracking-wide text-gray-500 dark:text-gray-400">강의 일정</h3>
          {blocks.length > 0 ? (
            <ul className="flex flex-wrap gap-2.5">
              {blocks.map(({ label, periodLabel }) => (
                <li key={label} className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-0.5 rounded-xl border border-gray-100 bg-gray-50 px-4 py-2 dark:border-gray-800 dark:bg-gray-800/50" data-clarity-unmask={clarityUnmask}>
                  <span className="font-mono text-sm font-bold tracking-tight wrap-anywhere text-gray-900 dark:text-gray-100">{label}</span>
                  {periodLabel && (
                    <span className="text-xs tabular-nums text-gray-500 dark:text-gray-400">{periodLabel}</span>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p data-clarity-unmask={clarityUnmask} className="w-fit max-w-full rounded-xl border border-gray-100 bg-gray-50 px-4 py-2 text-sm wrap-anywhere text-gray-500 italic dark:border-gray-800 dark:bg-gray-800/50 dark:text-gray-400">{fallback}</p>
          )}
        </div>

        <div className="min-w-0 border-t border-gray-100 pt-4 md:border-t-0 md:border-l md:pt-0 md:pl-8 dark:border-gray-700">
          <h3 className="mb-2 text-xs font-bold tracking-wide text-gray-500 dark:text-gray-400">강의실 위치</h3>
          <p className="flex items-start gap-1.5 text-xl leading-tight font-bold text-gray-900 dark:text-gray-100">
            <MapPin className="mt-1 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
            <span className="wrap-anywhere">{course.classroom || "강의실 미배정"}</span>
          </p>
          {mapQuery && (
            <details
              className="group mt-1.5"
              onToggle={(event) => setShowMap(event.currentTarget.open)}
            >
              <summary className="flex w-fit cursor-pointer list-none items-center gap-1 rounded-sm px-1 py-1 text-xs font-medium text-primary hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring [&::-webkit-details-marker]:hidden">
                지도 보기
                <ChevronDown className="size-3.5 group-open:rotate-180 motion-safe:transition-transform" aria-hidden="true" />
              </summary>
              {showMap && (
                <div className="mt-2 space-y-2">
                  {process.env.NEXT_PUBLIC_KAKAO_MAP_JS_KEY && (
                    <KakaoMapEmbed query={mapQuery} className="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700" />
                  )}
                  <a
                    href={`https://map.kakao.com/link/search/${encodeURIComponent(mapQuery)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                  >
                    카카오맵에서 열기
                    <ExternalLink className="size-3" aria-hidden="true" />
                  </a>
                </div>
              )}
            </details>
          )}
        </div>
      </div>
    </section>
  );
}

function CourseSeatStatus({ course }: { course: NormalizedCourse }) {
  const { current, capacity, available } = course;
  const isFull = available <= 0;
  const percent = capacity > 0 ? Math.min(100, (current / capacity) * 100) : 0;
  const roundedPercent = Math.round(percent);

  return (
    <section className="flex min-w-0 flex-col gap-3">
      <h2 className="flex items-center gap-2 text-base font-bold text-gray-900 dark:text-gray-100">
        <Users className="size-5 text-primary" aria-hidden="true" />
        신청 현황
      </h2>
      <div className="flex flex-1 flex-col items-center justify-center gap-6 rounded-3xl border border-primary/10 bg-linear-to-br from-[#f3eaf7] to-white p-6 shadow-sm dark:from-primary/10 dark:to-[#121212]">
        {capacity > 0 ? (
          <>
            <div
              role="progressbar"
              aria-label="신청률"
              aria-valuenow={roundedPercent}
              className="relative aspect-square w-36 max-w-full"
            >
              <svg className="size-full -rotate-90" viewBox="0 0 36 36" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
                <circle className="text-gray-200 dark:text-gray-700" cx="18" cy="18" r="16" />
                <circle className={isFull ? "text-red-500" : "text-primary"} cx="18" cy="18" r="16" pathLength="100" strokeDasharray={`${percent} 100`} strokeLinecap="round" />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span className="mb-0.5 text-xs font-semibold text-gray-500 dark:text-gray-400">신청률</span>
                <span className={cn("text-3xl font-black tracking-tight tabular-nums", isFull ? "text-red-600 dark:text-red-400" : "text-primary")}>
                  {roundedPercent}%
                </span>
              </div>
            </div>
            <dl className="grid w-full grid-cols-2 gap-px overflow-hidden rounded-xl border border-gray-200 bg-gray-200 text-center dark:border-gray-700 dark:bg-gray-700">
              <div className="min-w-0 bg-white p-3 dark:bg-gray-800">
                <dt className="mb-1 text-xs font-medium text-gray-500 dark:text-gray-400">신청 / 정원</dt>
                <dd className="text-lg font-bold wrap-anywhere tabular-nums text-gray-900 dark:text-gray-100">{current} / {capacity}</dd>
              </div>
              <div className="min-w-0 bg-white p-3 dark:bg-gray-800">
                <dt className="mb-1 text-xs font-medium text-gray-500 dark:text-gray-400">빈자리</dt>
                <dd className={cn("text-lg font-bold wrap-anywhere tabular-nums", isFull ? "text-red-600 dark:text-red-400" : "text-primary")}>
                  {available}
                </dd>
              </div>
            </dl>
            {course.lastCrawledAt && (
              <p className="-mt-3 flex items-center justify-center gap-1 text-xs font-medium text-gray-500 dark:text-gray-400">
                <Clock3 className="size-3 shrink-0" aria-hidden="true" />
                {formatRelativeTime(course.lastCrawledAt)} 업데이트
              </p>
            )}
          </>
        ) : (
          <div className="flex flex-col items-center gap-3 text-center text-gray-500 dark:text-gray-400">
            <AlertCircle className="size-10 opacity-50" aria-hidden="true" />
            <p className="text-sm font-medium">실시간 현황 데이터가 없습니다.</p>
          </div>
        )}
      </div>
    </section>
  );
}

function formatDepartmentLabel(department?: string, targetGradeLabel?: string): string {
  if (!department) {
    return "";
  }

  const normalizedDepartment = department.replace(/\s+/g, " ").trim();
  if (!normalizedDepartment) {
    return "";
  }

  const gradeNumber = extractGradeNumber(targetGradeLabel);
  if (!gradeNumber) {
    return normalizedDepartment;
  }

  return normalizedDepartment
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) =>
      part
        .replace(new RegExp(`\\s+${gradeNumber}(?=\\s*(?:학년)?(?:\\s*등)?\\s*$)`), "")
        .replace(new RegExp(`\\s+${gradeNumber}학년(?=\\s*(?:등)?\\s*$)`), "")
        .trim(),
    )
    .join(", ");
}

function extractGradeNumber(targetGradeLabel?: string): string | null {
  if (!targetGradeLabel) {
    return null;
  }

  const match = targetGradeLabel.match(/^([1-6])학년$/);
  return match ? match[1] : null;
}
