"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { Course } from "@/types/api";
import { Badge } from "@/components/ui/badge";
import { CalendarClock, GraduationCap, MapPin, Users, BookOpen, Clock, Globe } from "lucide-react";

interface CourseDetailDialogProps {
  course: Course | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CourseDetailDialog({ course, open, onOpenChange }: CourseDetailDialogProps) {
  if (!course) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl bg-card/95 backdrop-blur-3xl border-white/10 shadow-2xl overflow-hidden">
        <DialogHeader className="pb-4 border-b border-white/5 space-y-3">
            <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                    <span className="text-[10px] font-black uppercase text-primary tracking-widest bg-primary/10 px-2 py-0.5 rounded border border-primary/20">
                    {course.courseKey}
                    </span>
                    <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">
                    {course.academicYear} / {course.semester}
                    </span>
                </div>
                <DialogTitle className="text-2xl font-black bg-gradient-to-br from-foreground to-foreground/70 bg-clip-text text-transparent">
                    {course.name}
                </DialogTitle>
                <div className="flex items-center gap-2 text-sm text-foreground/80 font-medium">
                    <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-muted/50 border border-white/5">
                        <GraduationCap className="w-4 h-4 text-muted-foreground" />
                        {course.professor || "교수 미지정"}
                    </span>
                    <span className="text-muted-foreground/30">|</span>
                    <span className="text-muted-foreground">{course.department}</span>
                </div>
            </div>
          <DialogDescription className="sr-only">
            강좌 상세 정보
          </DialogDescription>
        </DialogHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-4">
            {/* Left Column: Core Info */}
            <div className="space-y-6">
                 {/* Classification & Credits */}
                <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase text-muted-foreground tracking-widest flex items-center gap-2">
                        <BookOpen className="w-3.5 h-3.5" />
                        Academic Info
                    </h3>
                    <div className="grid grid-cols-2 gap-3">
                        <InfoCard label="이수구분" value={course.classification} />
                        <InfoCard label="학점" value={course.credits} />
                        <InfoCard label="성적평가" value={course.gradingMethod} />
                        <InfoCard label="강의언어" value={course.lectureLanguage} icon={<Globe className="w-3 h-3 ml-1 inline text-muted-foreground/60"/>} />
                        <InfoCard label="대상학년" value={course.targetGrade} />
                        <InfoCard label="분반" value={course.classNumber} />
                    </div>
                </div>

                 {/* General Category */}
                 <div className="space-y-3">
                    <div className="flex items-center justify-between">
                         <h3 className="text-xs font-black uppercase text-muted-foreground tracking-widest">General Education</h3>
                         {course.generalCategory && <Badge variant="secondary" className="text-[10px] h-5">{course.generalCategory}</Badge>}
                    </div>
                    {course.generalDetail && (
                        <div className="bg-muted/30 p-3 rounded-xl border border-white/5 text-xs text-muted-foreground">
                            {course.generalDetail}
                        </div>
                    )}
                     {course.generalCategoryByYear && (
                        <div className="text-[10px] text-muted-foreground/50 px-1">
                            * 입학년도 기준: {course.generalCategoryByYear}
                        </div>
                    )}
                </div>
            </div>

            {/* Right Column: Schedule & Seat */}
            <div className="space-y-6">
                {/* Time & Place */}
                <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase text-muted-foreground tracking-widest flex items-center gap-2">
                        <CalendarClock className="w-3.5 h-3.5" />
                        Time & Place
                    </h3>
                    <div className="bg-muted/30 p-4 rounded-2xl border border-white/5 space-y-3">
                        <div className="flex items-start gap-4">
                            <Clock className="w-4 h-4 mt-0.5 text-primary/70" />
                            <div className="space-y-1">
                                <span className="text-xs font-bold text-muted-foreground uppercase tracking-wide block">Class Time</span>
                                <p className="text-sm font-semibold text-foreground leading-snug">{course.classTime || "정보 없음"}</p>
                                {course.classDuration && <span className="text-[10px] text-muted-foreground">{course.classDuration}</span>}
                            </div>
                        </div>
                        <div className="h-px bg-white/5" />
                        <div className="flex items-start gap-4">
                            <MapPin className="w-4 h-4 mt-0.5 text-primary/70" />
                            <div className="space-y-1">
                                <span className="text-xs font-bold text-muted-foreground uppercase tracking-wide block">Classroom</span>
                                <p className="text-sm font-semibold text-foreground leading-snug">{course.classroom || "강의실 미정"}</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Seat Info */}
                <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase text-muted-foreground tracking-widest flex items-center gap-2">
                        <Users className="w-3.5 h-3.5" />
                        Current Status
                    </h3>
                    <div className="grid grid-cols-3 gap-3">
                         <div className="flex flex-col items-center justify-center p-3 bg-muted/30 rounded-xl border border-white/5">
                             <span className="text-[10px] text-muted-foreground uppercase font-bold">Capacity</span>
                             <span className="text-lg font-black">{course.capacity}</span>
                         </div>
                         <div className="flex flex-col items-center justify-center p-3 bg-muted/30 rounded-xl border border-white/5">
                             <span className="text-[10px] text-muted-foreground uppercase font-bold">Current</span>
                             <span className="text-lg font-black">{course.current}</span>
                         </div>
                         <div className="flex flex-col items-center justify-center p-3 bg-primary/10 rounded-xl border border-primary/20">
                             <span className="text-[10px] text-primary uppercase font-bold">Available</span>
                             <span className="text-lg font-black text-primary">{course.available}</span>
                         </div>
                    </div>
                </div>
            </div>
        </div>

        {/* Footer info */}
        <div className="mt-8 pt-4 border-t border-white/5 flex justify-between items-center">
             <div className="flex gap-2">
                <Badge variant="outline" className="text-[10px] text-muted-foreground border-white/10 font-normal">
                    {course.hasSyllabus ? "강의계획서 O" : "강의계획서 X"}
                </Badge>
                <Badge variant="outline" className="text-[10px] text-muted-foreground border-white/10 font-normal">
                    {course.disclosure === "PUBLIC" ? "공개" : "비공개"} {course.disclosureReason && `(${course.disclosureReason})`}
                </Badge>
             </div>
             {course.courseDirection && (
                 <p className="text-[10px] text-muted-foreground max-w-[300px] truncate text-right">
                    방향: {course.courseDirection}
                 </p>
             )}
        </div>

      </DialogContent>
    </Dialog>
  );
}

function InfoCard({ label, value, icon }: { label: string, value?: string | number, icon?: React.ReactNode }) {
    return (
        <div className="flex flex-col justify-center p-2.5 rounded-lg bg-background/40 border border-white/5">
            <span className="text-[9px] font-bold text-muted-foreground/70 uppercase tracking-wider mb-0.5">{label}</span>
            <span className="text-xs font-bold text-foreground/90 truncate flex items-center">
                {value || "-"} {icon}
            </span>
        </div>
    )
}
