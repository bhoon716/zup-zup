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

  return (
    <div className="space-y-6 px-4 py-5 sm:px-7 sm:py-6">
      <header className="space-y-3 pr-8">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs font-medium text-muted-foreground">
          <span>{course.stdtrNo || "학수번호 미제공"}</span>
          {course.classification && <span className="text-primary">{formatClassification(course.classification)}</span>}
          {course.disclosure && (
            <span>
              {course.disclosure}
              {course.disclosure === "비공개" && course.disclosureReason && ` · ${course.disclosureReason}`}
            </span>
          )}
        </div>
        <h1 data-clarity-unmask={clarityUnmask} className="text-xl leading-snug font-semibold tracking-tight wrap-anywhere text-foreground sm:text-2xl">
          {course.name}
        </h1>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-1.5 font-medium text-foreground">
            <UserRound className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
            {course.professor}
          </span>
          {departmentLabel && <span>{departmentLabel} {targetGradeLabel === "전체" ? "" : targetGradeLabel}</span>}
          {!course.isSubscribable && (
            <span className="inline-flex items-center gap-1.5 text-xs">
              <AlertCircle className="size-3.5 shrink-0" aria-hidden="true" />
              조회 전용 (종료된 학기)
            </span>
          )}
        </div>
      </header>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-3 rounded-xl bg-muted/50 px-4 py-3 text-sm sm:grid-cols-4">
        <div>
          <dt className="mb-1 text-xs text-muted-foreground">학점 / 시간</dt>
          <dd data-clarity-unmask={clarityUnmask} className="font-medium text-foreground">
            {course.credits || "-"}학점
            {course.lectureHours ? <span className="font-normal text-muted-foreground"> · {course.lectureHours}시간</span> : null}
          </dd>
        </div>
        {[
          ["성적평가", formatGradingMethod(course.gradingMethod)],
          ["강의방식", course.courseStatus || "일반"],
          ["강의언어", formatLanguage(course.lectureLanguage)],
        ].map(([label, value]) => (
          <div key={label}>
            <dt className="mb-1 text-xs text-muted-foreground">{label}</dt>
            <dd className="font-medium text-foreground">{value}</dd>
          </div>
        ))}
      </dl>

      <div className="grid gap-5 md:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
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

function CourseTimeLocation({ course, clarityUnmask }: { course: Course; clarityUnmask?: true }) {
  const { blocks, fallback } = getCourseSchedule(course);
  const mapQuery = getCampusMapQuery(course.classroom);
  const [showMap, setShowMap] = useState(false);

  return (
    <section className="min-w-0 space-y-4 border-t border-border pt-4">
      <h2 className="flex items-center gap-2 text-base font-semibold text-foreground">
        <Clock3 className="size-4 text-primary" aria-hidden="true" />
        시간 및 장소
      </h2>

      <div>
        <h3 className="mb-1.5 text-xs font-medium text-muted-foreground">강의 일정</h3>
        {blocks.length > 0 ? (
          <ul className="space-y-1.5">
            {blocks.map(({ label, periodLabel }) => (
              <li key={label} className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5" data-clarity-unmask={clarityUnmask}>
                <span className="text-sm font-medium tabular-nums text-foreground">{label}</span>
                {periodLabel && (
                  <span className="text-xs tabular-nums text-muted-foreground">{periodLabel}</span>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p data-clarity-unmask={clarityUnmask} className="text-sm text-muted-foreground">{fallback}</p>
        )}
      </div>

      <div>
        <h3 className="mb-1.5 text-xs font-medium text-muted-foreground">강의실 위치</h3>
        <p className="flex items-start gap-1.5 text-sm font-medium text-foreground">
          <MapPin className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          {course.classroom || "강의실 미배정"}
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
                  <KakaoMapEmbed query={mapQuery} className="overflow-hidden rounded-lg border border-border" />
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
    </section>
  );
}

function CourseSeatStatus({ course }: { course: NormalizedCourse }) {
  const { current, capacity, available } = course;
  const isFull = available <= 0;
  const percent = capacity > 0 ? Math.min(100, (current / capacity) * 100) : 0;

  return (
    <section className="min-w-0 space-y-4 border-t border-border pt-4">
      <h2 className="flex items-center gap-2 text-base font-semibold text-foreground">
        <Users className="size-4 text-primary" aria-hidden="true" />
        신청 현황
      </h2>
      {capacity > 0 ? (
        <div className="space-y-2.5">
          <dl className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 text-sm">
            <div className="flex items-baseline gap-2">
              <dt className="text-xs text-muted-foreground">신청 / 정원</dt>
              <dd className="font-semibold tabular-nums text-foreground">{current} / {capacity}</dd>
            </div>
            <div className="flex items-baseline gap-2">
              <dt className="text-xs text-muted-foreground">잔여 여석</dt>
              <dd className={cn("font-semibold tabular-nums", isFull ? "text-red-600 dark:text-red-400" : "text-foreground")}>
                {available}
              </dd>
            </div>
          </dl>
          <div
            role="progressbar"
            aria-label="신청률"
            aria-valuenow={Math.round(percent)}
            className="h-1.5 overflow-hidden rounded-full bg-muted"
          >
            <div className={cn("h-full rounded-full", isFull ? "bg-red-500" : "bg-primary")} style={{ width: `${percent}%` }} />
          </div>
          <div className="flex flex-wrap justify-between gap-x-3 gap-y-1 text-xs tabular-nums text-muted-foreground">
            <span>신청률 {Math.round(percent)}%{isFull && " · 마감됨"}</span>
            {course.lastCrawledAt && <span>{formatRelativeTime(course.lastCrawledAt)} 업데이트</span>}
          </div>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">실시간 현황 데이터가 없습니다.</p>
      )}
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
