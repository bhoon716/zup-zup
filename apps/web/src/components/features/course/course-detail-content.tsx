"use client";

import type { Course } from "@/types/api";
import { Badge } from "@/components/ui/badge";
import { 
  CalendarClock, 
  GraduationCap, 
  MapPin, 
  Users, 
  BookOpen, 
  Clock, 
  Globe, 
  Award,
  Info,
  CheckCircle2,
  XCircle,
  Component
} from "lucide-react";
import { DialogTitle } from "@/components/ui/dialog";
import { formatClassification, formatGradingMethod, formatLanguage } from "@/lib/utils/formatters";

interface CourseDetailContentProps {
  course: Course;
  isDialog?: boolean;
}

export function CourseDetailContent({ course, isDialog = false }: CourseDetailContentProps) {
  const isAvailable = (course.available ?? 0) > 0;

  return (
    <div className="flex flex-col h-full overflow-y-auto">
      {/* Minimal Header - Clean Document Style */}
      <div className="px-8 pt-10 pb-6">
        <div className="flex flex-col gap-6">
          {/* Top Meta: Badges & Year */}
          <div className="flex items-center justify-between">
             <div className="flex items-center gap-3">
                <Badge variant="outline" className="font-mono text-[10px] font-normal border-primary/20 text-primary bg-primary/5 px-2 py-0.5 h-auto">
                    {course.courseKey}
                </Badge>
                <div className="h-3 w-[1px] bg-border/60" />
                <span className="text-xs text-muted-foreground font-medium tracking-wide">{course.academicYear}년 {course.semester}학기</span>
             </div>
             <div className={`
              flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors
              ${isAvailable 
                ? "bg-emerald-500/5 text-emerald-600 dark:text-emerald-400" 
                : "bg-destructive/5 text-destructive"}
            `}>
              <div className={`w-1.5 h-1.5 rounded-full ${isAvailable ? "bg-emerald-500" : "bg-destructive"}`} />
              <span>{isAvailable ? "신청가능" : "마감"}</span>
            </div>
          </div>

          {/* Main Title Area */}
          <div className="space-y-4">
            {isDialog ? (
                <DialogTitle className="text-3xl font-black tracking-tight text-foreground leading-tight break-keep">
                    {course.name}
                </DialogTitle>
            ) : (
                <h1 className="text-3xl font-black tracking-tight text-foreground leading-tight break-keep">
                    {course.name}
                </h1>
            )}
            
            {/* Sub Info Row */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-foreground/70">
                 <span className="font-semibold text-foreground">{course.professor || "교수 미지정"}</span>
                 <span className="text-border/40 text-[10px]">|</span>
                 <span>{course.department}</span>
                 <span className="text-border/40 text-[10px]">|</span>
                 <span>{formatClassification(course.classification)}</span>
                 {course.subjectCode && (
                   <>
                     <span className="text-border/40 text-[10px]">|</span>
                     <span className="font-mono text-xs text-muted-foreground">{course.subjectCode}</span>
                   </>
                 )}
            </div>
          </div>
        </div>
      </div>

      <div className="w-full h-[1px] bg-border/30 mx-auto max-w-[calc(100%-4rem)]" />

      {/* Content Body - Single Flow */}
      <div className="px-8 py-8 space-y-10">
        

        {/* Section 1: Key Statistics (Grid) */}
        <section>
            <h3 className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground/50 mb-4 select-none">상세 정보</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-8 gap-y-8">
                <SimpleDetailItem label="학점 · 시수" value={`${course.credits}학점 (${course.lectureHours || '-'}시수)`} />
                <SimpleDetailItem label="성적평가" value={formatGradingMethod(course.gradingMethod)} />
                <SimpleDetailItem label="대상학년" value={`${course.targetGrade}학년`} />
                <SimpleDetailItem label="강의언어" value={formatLanguage(course.lectureLanguage)} />
            </div>
            <div className="mt-6 pt-6 border-t border-border/40 grid grid-cols-2 gap-x-8">
                 <SimpleDetailItem label="이수구분" value={formatClassification(course.classification)} />
                 <SimpleDetailItem label="분반" value={`${course.classNumber}분반`} />
            </div>
        </section>

        {/* Section 2: Time & Location (Emphasis) */}
        <section>
            <h3 className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground/50 mb-4 select-none">시간 및 장소</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="flex flex-col gap-1">
                    <span className="text-2xl font-light tracking-tight">{course.classTime || "미정"}</span>
                    <span className="text-xs text-muted-foreground">{course.classDuration || "시간 정보 없음"}</span>
                </div>
                <div className="flex flex-col gap-1">
                     <span className="text-2xl font-light tracking-tight">{course.classroom || "미정"}</span>
                     <span className="text-xs text-muted-foreground">강의실 위치</span>
                </div>
            </div>
        </section>

        {/* Section 3: General Ed (Conditional) */}
        {(course.generalCategory || course.generalDetail) && (
            <section>
                 <h3 className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground/50 mb-4 select-none">교양 영역 정보</h3>
                 <div className="flex flex-wrap gap-4 text-sm bg-muted/20 p-4 rounded-lg border border-border/20">
                    {course.generalCategory && (
                        <div className="flex flex-col gap-0.5">
                            <span className="text-[10px] text-muted-foreground">영역</span>
                            <span className="font-medium">{course.generalCategory}</span>
                        </div>
                    )}
                    <div className="w-[1px] bg-border/40 mx-2" />
                    {course.generalDetail && (
                        <div className="flex flex-col gap-0.5">
                            <span className="text-[10px] text-muted-foreground">상세</span>
                            <span className="font-medium">{course.generalDetail}</span>
                        </div>
                    )}
                    {(course.generalCategoryByYear) && (
                         <>
                            <div className="w-[1px] bg-border/40 mx-2" />
                            <div className="flex flex-col gap-0.5">
                                <span className="text-[10px] text-muted-foreground">입학년도 기준</span>
                                <span className="font-medium text-muted-foreground">{course.generalCategoryByYear}</span>
                            </div>
                         </>
                    )}
                 </div>
            </section>
        )}

        {/* Section 4: Enrollment (Numbers) */}
        <section className="pt-2">
            <h3 className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground/50 mb-4 select-none">수강신청 현황</h3>
            <div className="grid grid-cols-3 gap-8 pb-4">
                <div className="space-y-1">
                    <p className="text-3xl font-normal tabular-nums tracking-tight">{course.capacity}</p>
                    <span className="text-[10px] text-muted-foreground font-medium">정원</span>
                </div>
                <div className="space-y-1">
                    <p className="text-3xl font-normal tabular-nums tracking-tight">{course.current}</p>
                    <span className="text-[10px] text-muted-foreground font-medium">현재</span>
                </div>
                <div className="space-y-1">
                    <p className={`text-3xl font-semibold tabular-nums tracking-tight ${isAvailable ? 'text-emerald-600' : 'text-muted-foreground'}`}>{course.available}</p>
                    <span className={`text-[10px] font-medium ${isAvailable ? 'text-emerald-600' : 'text-muted-foreground'}`}>여석</span>
                </div>
            </div>
        </section>
      </div>

      {/* Footer */}
      <div className="px-8 py-6 border-t border-border/40 flex items-center justify-between text-xs text-muted-foreground mt-auto bg-background/50">
        <div className="flex gap-6">
            <span className="flex items-center gap-1.5 hover:text-foreground transition-colors cursor-help" title="강의계획서 조회 가능 여부">
                {course.hasSyllabus ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" /> : <XCircle className="w-3.5 h-3.5 text-muted-foreground/50" />}
                강의계획서
            </span>
            <span className="flex items-center gap-1.5 hover:text-foreground transition-colors cursor-help">
                {course.disclosure === "PUBLIC" ? <Globe className="w-3.5 h-3.5" /> : <XCircle className="w-3.5 h-3.5 text-muted-foreground/50" />}
                {course.disclosure === "PUBLIC" ? "공개 강좌" : "비공개"}
                {course.disclosureReason && ` (${course.disclosureReason})`}
            </span>
        </div>
        {course.courseDirection && (
            <span className="truncate max-w-[300px] text-muted-foreground/80" title={course.courseDirection}>
                {course.courseDirection}
            </span>
        )}
      </div>
    </div>
  );
}

function SimpleDetailItem({ label, value }: { label: string, value?: string | number }) {
    return (
        <div className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold text-muted-foreground/60 uppercase tracking-wider">{label}</span>
            <span className="text-sm font-medium text-foreground break-keep leading-relaxed">{value || "-"}</span>
        </div>
    )
}
