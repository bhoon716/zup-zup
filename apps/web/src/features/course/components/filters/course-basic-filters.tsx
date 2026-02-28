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
}

export function CourseBasicFilters({
  condition,
  setCondition,
}: CourseBasicFiltersProps) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">학년도</Label>
          <Select
            value={condition.academicYear}
            onValueChange={(value) =>
              setCondition((prev) => ({ ...prev, academicYear: value }))
            }
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
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
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              {SEMESTERS.map((sem) => (
                <SelectItem key={sem.value} value={sem.value}>
                  {sem.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">교수명</Label>
        <Input
          value={condition.professor || ""}
          onChange={(e) =>
            setCondition((prev) => ({ ...prev, professor: e.target.value }))
          }
          placeholder="예: 홍길동"
          className="h-10 rounded-xl bg-muted/30"
        />
      </div>

      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">학과 검색</Label>
        <Input
          value={condition.department || ""}
          onChange={(e) =>
            setCondition((prev) => ({ ...prev, department: e.target.value }))
          }
          placeholder="예: 컴퓨터공학부, 경영학과"
          className="h-10 rounded-xl bg-muted/30"
        />
      </div>
    </div>
  );
}
