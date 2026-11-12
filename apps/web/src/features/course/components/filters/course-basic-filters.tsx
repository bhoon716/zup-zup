"use client";

import { useEffect, useRef, useState } from "react";
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
  const professorId = `${idBasePrefix}-professor`;
  const departmentId = `${idBasePrefix}-department`;

  const [localProf, setLocalProf] = useState(() => condition.professor || "");
  const [localDept, setLocalDept] = useState(() => condition.department || "");
  const isProfFocusedRef = useRef(false);
  const isDeptFocusedRef = useRef(false);

  useEffect(() => {
    if (!isProfFocusedRef.current) {
      setLocalProf(condition.professor || "");
    }
  }, [condition.professor]);

  useEffect(() => {
    if (!isDeptFocusedRef.current) {
      setLocalDept(condition.department || "");
    }
  }, [condition.department]);

  const handleProfChange = (val: string) => {
    setLocalProf(val);
    setCondition((prev) => ({ ...prev, professor: val || undefined }));
  };

  const handleDeptChange = (val: string) => {
    setLocalDept(val);
    setCondition((prev) => ({ ...prev, department: val || undefined }));
  };

  return (
    <div className="space-y-3">
      {                           }
      <div className="flex flex-col gap-3">
        <div className="space-y-1.5">
          <Label htmlFor={academicYearSelectId} className="text-xs font-medium text-muted-foreground">학년도</Label>
          <Select
            value={condition.academicYear}
            onValueChange={(value) =>
              setCondition((prev) => ({ ...prev, academicYear: value }))
            }
          >
            <SelectTrigger id={academicYearSelectId} aria-controls={`${academicYearSelectId}-content`} className="h-10 w-full rounded-lg bg-card text-sm">
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
          <Label htmlFor={semesterSelectId} className="text-xs font-medium text-muted-foreground">학기</Label>
          <Select
            value={condition.semester}
            onValueChange={(value) =>
              setCondition((prev) => ({ ...prev, semester: value }))
            }
          >
            <SelectTrigger id={semesterSelectId} aria-controls={`${semesterSelectId}-content`} className="h-10 w-full rounded-lg bg-card text-sm">
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

      {                                  }
      <div className="flex flex-col gap-3">
        <div className="space-y-1.5">
          <Label htmlFor={professorId} className="text-xs font-medium text-muted-foreground">교수명</Label>
          <Input
            id={professorId}
            aria-describedby={`${professorId}-hint`}
            value={localProf}
            onChange={(e) => handleProfChange(e.target.value)}
            onFocus={() => {
              isProfFocusedRef.current = true;
            }}
            onBlur={() => {
              isProfFocusedRef.current = false;
            }}
            placeholder="예: 홍길동, 김철수"
            className="h-10 rounded-lg bg-card"
          />
          <p id={`${professorId}-hint`} className="text-xs leading-relaxed text-muted-foreground">
            여러 교수명을 쉼표로 구분해 입력할 수 있습니다.
          </p>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor={departmentId} className="text-xs font-medium text-muted-foreground">학과명</Label>
          <Input
            id={departmentId}
            aria-describedby={`${departmentId}-hint`}
            value={localDept}
            onChange={(e) => handleDeptChange(e.target.value)}
            onFocus={() => {
              isDeptFocusedRef.current = true;
            }}
            onBlur={() => {
              isDeptFocusedRef.current = false;
            }}
            placeholder="예: 소프트웨어, 컴퓨터공학부"
            className="h-10 rounded-lg bg-card"
          />
          <p id={`${departmentId}-hint`} className="text-xs leading-relaxed text-muted-foreground">
            여러 학과명을 쉼표로 구분해 입력할 수 있습니다.
          </p>
        </div>
      </div>
    </div>
  );
}
