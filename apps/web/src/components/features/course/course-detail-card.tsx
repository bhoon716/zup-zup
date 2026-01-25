"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { User, BookOpen, GraduationCap, Users } from "lucide-react";
import type { Course } from "@/types/api";

interface CourseDetailCardProps {
  course: Course;
}

export function CourseDetailCard({ course }: CourseDetailCardProps) {
  const isAvailable = course.status === "AVAILABLE";

  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <CardTitle className="text-2xl font-bold">{course.name}</CardTitle>
            <CardDescription className="flex items-center gap-2">
              <span className="font-mono text-sm">{course.courseKey}</span>
              <Badge variant={isAvailable ? "default" : "secondary"} className={isAvailable ? "bg-green-600 hover:bg-green-700" : ""}>
                {isAvailable ? "여석 있음" : "만석"}
              </Badge>
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-100 dark:bg-blue-900 rounded-lg">
            <User className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">담당 교수</p>
            <p className="font-medium">{course.professorName}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="p-2 bg-purple-100 dark:bg-purple-900 rounded-lg">
            <BookOpen className="w-5 h-5 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">과목 코드</p>
            <p className="font-medium">{course.subjectCode}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="p-2 bg-orange-100 dark:bg-orange-900 rounded-lg">
            <GraduationCap className="w-5 h-5 text-orange-600 dark:text-orange-400" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">대상 학년</p>
            <p className="font-medium">{course.targetGrade}학년</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="p-2 bg-green-100 dark:bg-green-900 rounded-lg">
            <Users className="w-5 h-5 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">인원 현황</p>
            <p className="font-medium">
              {course.currentSeats} / {course.totalSeats} 
              <span className="ml-2 text-sm text-muted-foreground">
                ({course.totalSeats - course.currentSeats}석 남음)
              </span>
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
