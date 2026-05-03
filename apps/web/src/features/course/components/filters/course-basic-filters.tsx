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
import { useCollegeHierarchy } from "../../hooks/useCourses";

interface CourseBasicFiltersProps {
  condition: CourseSearchCondition;
  setCondition: React.Dispatch<React.SetStateAction<CourseSearchCondition>>;
}

export function CourseBasicFilters({
  condition,
  setCondition,
}: CourseBasicFiltersProps) {
  const { data: hierarchy, isLoading: isHierarchyLoading } = useCollegeHierarchy();

  // 선택된 단과대에 해당하는 학과 목록 추출
  const selectedCollege = hierarchy?.find(c => c.id === condition.collegeId);
  const departments = selectedCollege?.departments || [];

  return (
    <div className="space-y-4">
      {/* 학년도 및 학기 */}
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

      {/* 계층형 학과 선택 (핵심 기능) */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">단과대 / 학과 선택</Label>
        <div className="flex flex-col gap-2">
          <Select
            disabled={isHierarchyLoading}
            value={condition.collegeId?.toString() || "all"}
            onValueChange={(value) => {
              const collegeId = value === "all" ? undefined : parseInt(value);
              setCondition(prev => ({
                ...prev,
                collegeId,
                departmentId: undefined // 단과대 변경 시 학과 초기화
              }));
            }}
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder={isHierarchyLoading ? "로딩 중..." : "- 단과대 선택 -"} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 전체 단과대 -</SelectItem>
              {hierarchy?.map((college) => (
                <SelectItem key={college.id} value={college.id.toString()}>
                  {college.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select
            disabled={!condition.collegeId}
            value={condition.departmentId?.toString() || "all"}
            onValueChange={(value) => {
              const departmentId = value === "all" ? undefined : parseInt(value);
              setCondition(prev => ({ ...prev, departmentId }));
            }}
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder={!condition.collegeId ? "- 단과대를 먼저 선택하세요 -" : "- 학과 선택 -"} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 전체 학과 -</SelectItem>
              {departments.map((dept) => (
                <SelectItem key={dept.id} value={dept.id.toString()}>
                  {dept.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 교수명 및 학과명 직접 입력 (보조 기능) */}
      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">교수명</Label>
          <Input
            value={condition.professor || ""}
            onChange={(e) =>
              setCondition((prev) => ({ ...prev, professor: e.target.value }))
            }
            placeholder="예: 홍길동"
            className="h-10 rounded-xl bg-muted/30 text-xs"
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
            className="h-10 rounded-xl bg-muted/30 text-xs"
          />
        </div>
      </div>
    </div>
  );
}
