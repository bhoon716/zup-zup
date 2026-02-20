"use client";

import { AlertCircle, Clock3, User, Users } from "lucide-react";
import type { Course } from "@/shared/types/api";
import { formatClassification, formatGradingMethod, formatLanguage, formatTargetGrade } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { normalizeCourse } from "@/shared/lib/course";
import { getCampusMapQuery } from "@/shared/lib/map-links";
import { KakaoMapEmbed } from "@/shared/ui/kakao-map-embed";

interface CourseDetailContentProps {
  course: Course;
}

/**
 * 강의 상세 정보를 보여주는 공통 콘텐츠 컴포넌트입니다.
 * 다이얼로그나 모달 내부에서 사용됩니다.
 */
export function CourseDetailContent({ course: rawCourse }: CourseDetailContentProps) {
  const course = normalizeCourse(rawCourse);
  const isFull = (course.available ?? 0) <= 0;
  const capacity = course.capacity ?? 0;
  const current = course.current ?? 0;
  const available = course.available ?? 0;
  const percent = capacity > 0 ? Math.min(100, (current / capacity) * 100) : 0;
  const mapQuery = getCampusMapQuery(course.classroom) ?? "전북대학교 전주캠퍼스";

  const classLabel = (() => {
    const academicYear = course.academicYear ? `${course.academicYear}년 ` : "";
    const semester = course.semester ? `${course.semester}학기 · ` : "";
    const subjectCode = course.subjectCode || course.courseKey;
    const classNumber = course.classNumber ? `-${course.classNumber}` : "";
    return `${academicYear}${semester}${subjectCode}${classNumber}`.trim();
  })();

  return (
    <div className="px-6 md:px-8 py-6 space-y-6 bg-white dark:bg-[#121212]">
      {/* Header Info */}
      <div className="flex flex-col gap-4 w-full">
        <div className="flex flex-wrap items-center gap-2.5 text-sm">
          <span className="px-3 py-1 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 rounded-full font-medium text-xs tracking-tight border border-gray-200 dark:border-gray-700">
            {classLabel}
          </span>
          {isFull && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-full text-xs font-bold border border-red-100 dark:border-red-900/30">
              <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
              마감됨
            </span>
          )}
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <div className="relative pl-3">
              <div className="absolute left-0 top-1 bottom-1 w-1 rounded-full bg-primary/70"></div>
              <h1 className="text-3xl md:text-4xl font-black text-gray-900 dark:text-gray-100 tracking-tight leading-tight">
                {course.name}
              </h1>
            </div>

            <div className="flex flex-wrap items-center gap-3 mt-3 text-base text-gray-600 dark:text-gray-400">
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800">
                <User className="w-4 h-4 text-primary" />
                <span className="font-bold text-gray-900 dark:text-gray-100 text-sm">
                  {course.professor || "교수 미지정"}
                </span>
              </div>

              {course.department && (
                <>
                  <span className="text-sm text-gray-300 dark:text-gray-600">|</span>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {course.department} {(() => {
                      const tg = formatTargetGrade(course.targetGrade);
                      return tg === '전체' ? '' : tg;
                    })()}
                  </span>
                </>
              )}

              {course.classification && (
                <>
                  <span className="text-sm text-gray-300 dark:text-gray-600">|</span>
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-primary/10 text-primary dark:text-primary-light">
                    {formatClassification(course.classification)}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Summary Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <DetailBox label="학점 / 시간" value={`${course.credits || "-"}학점`} subValue={course.lectureHours ? ` (${course.lectureHours}시간)` : ""} />
        <DetailBox label="성적평가" value={formatGradingMethod(course.gradingMethod)} />
        <DetailBox label="수업방향" value={course.courseDirection || "-"} />
        <DetailBox label="강의언어" value={formatLanguage(course.lectureLanguage)} />
      </div>

      {/* Bottom Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Time & Location */}
        <div className="lg:col-span-2 flex flex-col h-full">
          <div className="flex items-center gap-2 mb-3">
            <Clock3 className="w-5 h-5 text-primary" />
            <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">시간 및 장소</h2>
          </div>
          <div className="bg-white dark:bg-[#1E1E1E] border border-gray-200 dark:border-gray-700 rounded-3xl p-6 shadow-sm flex flex-col md:flex-row gap-8 flex-1">
            <div className="flex-1 space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wide">강의 일정</span>
                {course.classDuration && (
                  <span className="text-xs font-medium text-primary bg-primary/5 px-2 py-1 rounded">
                    {course.classDuration} 수업
                  </span>
                )}
              </div>
              <div className="space-y-3">
                <div className="flex items-start gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800">
                  <span className="flex shrink-0 items-center justify-center w-8 h-8 rounded-lg bg-white dark:bg-gray-700 text-sm font-bold text-gray-700 dark:text-gray-200 shadow-sm border border-gray-100 dark:border-gray-600">
                    강
                  </span>
                  <div className="flex flex-col">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100 font-mono">
                      {course.classTime || "-"}
                    </span>
                    <span className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                      원규 전체 시간표: {course.classTime || "-"}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="w-px bg-gray-100 dark:bg-gray-700 hidden md:block"></div>

            <div className="flex-1 flex flex-col justify-between space-y-4">
              <div>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wide block mb-2">강의실 위치</span>
                <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 leading-tight">
                  {course.classroom || "장소 미정"}
                </h3>
              </div>
              <KakaoMapEmbed
                query={mapQuery}
                className="rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700"
              />
            </div>
          </div>
        </div>

        {/* Application Status */}
        <div className="flex flex-col h-full">
          <div className="flex items-center gap-2 mb-3">
            <Users className="w-5 h-5 text-primary" />
            <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">신청 현황</h2>
          </div>
          <div className="bg-linear-to-br from-[#f3eaf7] to-white dark:from-primary/10 dark:to-[#121212] border border-primary/10 rounded-3xl p-6 h-full flex flex-col justify-center items-center relative overflow-hidden shadow-sm">
            {capacity > 0 ? (
              <>
                <div className="relative w-36 h-36 mb-6">
                  <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                    <path className="text-gray-200 dark:text-gray-700" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="3"></path>
                    <path className={isFull ? "text-red-500" : "text-primary"} d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" strokeDasharray={`${percent}, 100`} strokeLinecap="round" strokeWidth="3"></path>
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                    <span className="text-xs font-semibold text-gray-500 mb-0.5">신청률</span>
                    <span className={cn("text-3xl font-black tracking-tight", isFull ? "text-red-500" : "text-primary dark:text-[#7e4d9a]")}>
                      {Math.round(percent)}%
                    </span>
                  </div>
                </div>
                <div className="w-full grid grid-cols-2 gap-px bg-gray-200 dark:bg-gray-700 rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700">
                  <div className="bg-white dark:bg-gray-800 p-3 text-center">
                    <div className="text-xs text-gray-500 dark:text-gray-400 font-medium mb-1">신청 / 정원</div>
                    <div className="text-lg font-bold text-gray-900 dark:text-gray-100">{current} / {capacity}</div>
                  </div>
                  <div className="bg-white dark:bg-gray-800 p-3 text-center">
                    <div className="text-xs text-gray-500 dark:text-gray-400 font-medium mb-1">잔여 여석</div>
                    <div className={cn("text-lg font-bold", isFull ? "text-red-500" : "text-primary")}>
                      {available}
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-center text-gray-400">
                <AlertCircle className="w-10 h-10 mb-3 opacity-50" />
                <p className="text-sm font-medium">실시간 현황<br />데이터가 없습니다</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function DetailBox({ label, value, subValue }: { label: string; value: string; subValue?: string }) {
  return (
    <div className="bg-gray-50 dark:bg-white/5 border border-gray-100 dark:border-white/10 rounded-2xl p-4 flex flex-col items-center justify-center text-center gap-1 hover:bg-primary/5 hover:border-primary/20 transition-colors group">
      <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1">{label}</span>
      <div className="text-lg font-bold text-gray-900 dark:text-gray-100 group-hover:text-primary transition-colors">
        {value}
        {subValue && <span className="text-sm font-normal text-gray-400">{subValue}</span>}
      </div>
    </div>
  );
}
