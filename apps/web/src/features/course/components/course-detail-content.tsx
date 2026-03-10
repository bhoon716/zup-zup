"use client";

import { AlertCircle, Clock3, User, Users } from "lucide-react";
import type { Course } from "@/shared/types/api";
import { formatClassification, formatDayOfWeek, formatGradingMethod, formatLanguage, formatRelativeTime, formatTargetGrade } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { normalizeCourse } from "@/shared/lib/course";
import { getCampusMapQuery } from "@/shared/lib/map-links";
import { KakaoMapEmbed } from "@/shared/ui/kakao-map-embed";
import { CourseReviewSection } from "@/features/review/components/course-review-section";

interface CourseDetailContentProps {
  course: Course;
}

/**
 * 강의 상세 정보를 보여주는 공통 콘텐츠 컴포넌트입니다.
 * 다이얼로그나 모달 내부에서 사용됩니다.
 */
export function CourseDetailContent({ course: rawCourse }: CourseDetailContentProps) {
  const course = normalizeCourse(rawCourse);
  const capacity = course.capacity ?? 0;
  const current = course.current ?? 0;
  const available = course.available ?? 0;
  const percent = capacity > 0 ? Math.min(100, (current / capacity) * 100) : 0;
  const mapQuery = getCampusMapQuery(course.classroom) ?? "전북대학교 전주캠퍼스";
  const targetGradeLabel = formatTargetGrade(course.targetGrade);
  const departmentLabel = formatDepartmentLabel(course.department, targetGradeLabel);

  /**
   * 학년도, 학기, 과목코드, 분반 정보를 조합하여 강의 식별용 라벨을 생성합니다.
   */
  const classLabel = (() => {
    const academicYear = course.academicYear ? `${course.academicYear}년 ` : "";
    const semester = course.semester ? `${course.semester}학기 · ` : "";
    const subjectCode = course.subjectCode || course.courseKey;
    const classNumber = course.classNumber ? `-${course.classNumber}` : "";
    return `${academicYear}${semester}${subjectCode}${classNumber}`.trim();
  })();

  return (
    <div className="px-6 md:px-8 py-6 space-y-6 bg-white dark:bg-[#121212]">
      <CourseHeader 
        course={course} 
        classLabel={classLabel} 
        departmentLabel={departmentLabel} 
        targetGradeLabel={targetGradeLabel} 
      />

      <CourseSummaryGrid course={course} />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <CourseTimeLocation 
          course={course} 
          mapQuery={mapQuery} 
        />
        <CourseSeatStatus 
          course={course} 
          percent={percent} 
          available={available} 
        />
      </div>

      <CourseReviewSection 
        courseKey={course.courseKey} 
        isReviewed={course.isReviewed} 
      />
    </div>
  );
}

/**
 * 강의 상단 기본 정보 섹션
 */
function CourseHeader({ 
  course, 
  classLabel, 
  departmentLabel, 
  targetGradeLabel 
}: { 
  course: Course; 
  classLabel: string; 
  departmentLabel: string; 
  targetGradeLabel: string;
}) {
  const isFull = (course.available ?? 0) <= 0;

  return (
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
        {!course.isSubscribable && (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 rounded-full text-xs font-bold border border-gray-200 dark:border-gray-700">
            <AlertCircle className="w-3 h-3" />
            조회 전용 (종료된 학기)
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
                  {departmentLabel} {targetGradeLabel === "전체" ? "" : targetGradeLabel}
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

            {course.disclosure && (
              <>
                <span className="text-sm text-gray-300 dark:text-gray-600">|</span>
                <span className={cn(
                  "inline-flex items-center px-2 py-0.5 rounded text-xs font-bold",
                  course.disclosure === "공개" 
                  ? "bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400" 
                  : "bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400"
                )}>
                  {course.disclosure}
                  {course.disclosure === "비공개" && course.disclosureReason && (
                    <span className="ml-1 opacity-70 font-medium">({course.disclosureReason})</span>
                  )}
                </span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * 학점, 성격평가 등 강의 메타 정보 요약 그리드
 */
function CourseSummaryGrid({ course }: { course: Course }) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
      <DetailBox label="학점 / 시간" value={`${course.credits || "-"}학점`} subValue={course.lectureHours ? ` (${course.lectureHours}시간)` : ""} />
      <DetailBox label="성적평가" value={formatGradingMethod(course.gradingMethod)} />
      <DetailBox label="강의방식" value={course.courseStatus || "일반"} />
      <DetailBox label="강의언어" value={formatLanguage(course.lectureLanguage)} />
    </div>
  );
}

/**
 * 강의 일정 및 위치 정보 섹션
 */
function CourseTimeLocation({ course, mapQuery }: { course: Course; mapQuery: string }) {
  return (
    <div className="lg:col-span-2 flex flex-col h-full">
      <div className="flex items-center gap-2 mb-3">
        <Clock3 className="w-5 h-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">시간 및 장소</h2>
      </div>
      <div className="bg-white dark:bg-[#1E1E1E] border border-gray-200 dark:border-gray-700 rounded-3xl p-6 shadow-sm flex flex-col md:flex-row gap-8 flex-1">
        <div className="flex-1 space-y-4">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wide">강의 일정</span>
          <div className="flex flex-wrap gap-2.5">
            {course.schedules && course.schedules.length > 0 ? (
              course.schedules.map((s, idx) => (
                <div key={idx} className="px-4 py-2 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-100 dark:border-gray-800 transition-colors hover:border-primary/30">
                  <span className="text-sm font-bold text-gray-900 dark:text-gray-100 font-mono tracking-tight">
                    {formatDayOfWeek(s.dayOfWeek)} {s.startTime} ~ {s.endTime}
                  </span>
                </div>
              ))
            ) : (
              <div className="px-4 py-2 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-100 dark:border-gray-800 italic text-gray-400 text-sm">
                {course.classTime || "시간 미배정"}
              </div>
            )}
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
  );
}

/**
 * 수강 신청 현황 (인원 및 신청률) 섹션
 */
function CourseSeatStatus({ course, percent, available }: { course: Course; percent: number; available: number }) {
  const isFull = available <= 0;
  const { current = 0, capacity = 0 } = course;

  return (
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
            <div className="w-full flex flex-col gap-3">
              <div className="grid grid-cols-2 gap-px bg-gray-200 dark:bg-gray-700 rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700">
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
              <div className="text-center">
                <span className="text-[10px] text-gray-400 font-medium flex items-center justify-center gap-1">
                  <Clock3 className="w-3 h-3" />
                  {formatRelativeTime(course.lastCrawledAt)} 업데이트
                </span>
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
  );
}

/**
 * 학과명에서 학년 정보를 제거하고 정규화된 학과 라벨을 반환합니다.
 */
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

/**
 * 학년 라벨(예: "3학년")에서 숫자(예: "3")만 추출합니다.
 */
function extractGradeNumber(targetGradeLabel?: string): string | null {
  if (!targetGradeLabel) {
    return null;
  }

  const match = targetGradeLabel.match(/^([1-6])학년$/);
  return match ? match[1] : null;
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
