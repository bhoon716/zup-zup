"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { User, BookOpen, GraduationCap, Users } from "lucide-react";
import type { Course } from "@/types/api";

interface CourseDetailCardProps {
  course: Course;
}

export function CourseDetailCard({ course }: CourseDetailCardProps) {
  const isAvailable = (course.available ?? 0) > 0;

  return (
    <div className="space-y-6">
      <Card className="w-full border-none shadow-xl bg-card/40 backdrop-blur-xl ring-1 ring-white/10 overflow-hidden">
        <div className="absolute top-0 left-0 w-1 h-full bg-primary" />
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="bg-primary/5 text-primary border-primary/20 font-bold px-2.5">
                  {course.classification || "기타"}
                </Badge>
                <span className="font-mono text-[10px] text-muted-foreground uppercase tracking-widest">{course.academicYear} / {course.semester}학기</span>
              </div>
              <CardTitle className="text-3xl font-black tracking-tight">{course.name}</CardTitle>
              <CardDescription className="flex items-center gap-2">
                <span className="font-mono text-sm font-medium bg-muted px-2 py-0.5 rounded">{course.subjectCode}</span>
                <span className="text-muted-foreground">|</span>
                <span className="text-sm font-medium">{course.department}</span>
              </CardDescription>
            </div>
            <div className="text-right">
              <Badge 
                className={`text-lg px-4 py-1.5 rounded-full font-black ${
                  isAvailable 
                    ? "bg-green-500/10 text-green-500 border border-green-500/20 shadow-[0_0_20px_rgba(34,197,94,0.15)]" 
                    : "bg-destructive/10 text-destructive border border-destructive/20"
                }`}
              >
                {isAvailable ? "OPEN" : "CLOSED"}
              </Badge>
              <p className="text-[10px] mt-2 font-bold text-muted-foreground uppercase tracking-tighter">Current Enrollment Status</p>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors">
              <div className="p-3 bg-blue-500/10 rounded-xl ring-1 ring-blue-500/20">
                <User className="w-6 h-6 text-blue-500" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-muted-foreground uppercase mb-0.5">Professor</p>
                <p className="text-lg font-bold">{course.professor || "미지정"}</p>
              </div>
            </div>

            <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors">
              <div className="p-3 bg-indigo-500/10 rounded-xl ring-1 ring-indigo-500/20">
                <BookOpen className="w-6 h-6 text-indigo-500" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-muted-foreground uppercase mb-0.5">Academic Info</p>
                <p className="text-lg font-bold">{course.credits}학점 <span className="text-sm font-normal text-muted-foreground">({course.gradingMethod})</span></p>
              </div>
            </div>

            <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors">
              <div className="p-3 bg-orange-500/10 rounded-xl ring-1 orange-500/20">
                <GraduationCap className="w-6 h-6 text-orange-500" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-muted-foreground uppercase mb-0.5">Target Grade</p>
                <p className="text-lg font-bold">{course.targetGrade || "전공"}</p>
              </div>
            </div>

            <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 border border-white/5 hover:bg-white/10 transition-colors">
              <div className="p-3 bg-emerald-500/10 rounded-xl ring-1 ring-emerald-500/20">
                <Users className="w-6 h-6 text-emerald-500" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-muted-foreground uppercase mb-0.5">Seats Availability</p>
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-black">{course.available || 0}</span>
                  <span className="text-xs text-muted-foreground">/ {course.capacity || 0} TOTAL</span>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-6 rounded-2xl bg-muted/30 border border-white/5">
              <h4 className="text-xs font-black uppercase tracking-widest text-muted-foreground mb-4">Class Schedule</h4>
              <div className="flex flex-wrap gap-2">
                {course.schedules && course.schedules.length > 0 ? (
                  course.schedules.map((s, i) => (
                    <Badge key={i} variant="secondary" className="px-3 py-1 text-sm font-mono">
                      {s.dayOfWeek} {s.period}
                    </Badge>
                  ))
                ) : (
                  <span className="text-sm text-muted-foreground">{course.classTime || "정보 없음"}</span>
                )}
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-muted/30 border border-white/5">
              <h4 className="text-xs font-black uppercase tracking-widest text-muted-foreground mb-4">Additional Details</h4>
              <div className="grid grid-cols-2 gap-y-4">
                <div>
                  <p className="text-[10px] font-bold text-muted-foreground uppercase">Language</p>
                  <p className="text-sm font-medium">{course.lectureLanguage || "한국어"}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold text-muted-foreground uppercase">Duration</p>
                  <p className="text-sm font-medium">{course.classDuration || "-"}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold text-muted-foreground uppercase">Syllabus</p>
                  <Badge variant={course.hasSyllabus ? "default" : "secondary"} className="text-[10px] h-5">
                    {course.hasSyllabus ? "등록됨" : "미등록"}
                  </Badge>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
