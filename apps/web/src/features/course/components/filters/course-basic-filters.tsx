"use client";

import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { YEARS, SEMESTERS } from "../../constants/course-options";
import type { CourseSearchCondition } from "@/shared/types/api";

interface CourseBasicFiltersProps {
  condition: CourseSearchCondition;
  setCondition: React.Dispatch<React.SetStateAction<CourseSearchCondition>>;
  idBasePrefix?: string;
}

export function CourseBasicFilters({
  condition,
  setCondition,
  idBasePrefix = "course-basic",
}: CourseBasicFiltersProps) {
  const academicYearSelectId = `${idBasePrefix}-academic-year-select`;
  const semesterSelectId = `${idBasePrefix}-semester-select`;
  return (
    <div className="space-y-4">
      {/* 학년도 및 학기 (세로 1줄 레이아웃) */}
      <div className="flex flex-col gap-4">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">학년도</Label>
          <Select
            value={condition.academicYear}
            onValueChange={(value) =>
              setCondition((prev) => ({ ...prev, academicYear: value }))
            }
          >
            <SelectTrigger id={academicYearSelectId} aria-controls={`${academicYearSelectId}-content`} className="h-10 w-full rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent id={`${academicYearSelectId}-content`}>
              {YEARS.map((year) => (
                <SelectItem key={year} value={year}>
                  {year}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">학기</Label>
          <Select
            value={condition.semester}
            onValueChange={(value) =>
              setCondition((prev) => ({ ...prev, semester: value }))
            }
          >
            <SelectTrigger id={semesterSelectId} aria-controls={`${semesterSelectId}-content`} className="h-10 w-full rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent id={`${semesterSelectId}-content`}>
              {SEMESTERS.map((sem) => (
                <SelectItem key={sem.value} value={sem.value}>
                  {sem.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 교수명 및 학과명 직접 입력 (세로 1줄 레이아웃) */}
      <div className="flex flex-col gap-4">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">교수명</Label>
          <Input
            value={condition.professor || ""}
            onChange={(e) =>
              setCondition((prev) => ({ ...prev, professor: e.target.value }))
            }
            placeholder="예: 홍길동"
            className="h-10 w-full rounded-xl bg-muted/30 text-xs"
          />
        </div>
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">학과명 직접 입력</Label>
          <Input
            value={condition.department || ""}
            onChange={(e) =>
              setCondition((prev) => ({ ...prev, department: e.target.value }))
            }
            placeholder="예: 소프트웨어"
            className="h-10 w-full rounded-xl bg-muted/30 text-xs"
          />
        </div>
      </div>
    </div>
  );
}
