"use client";

import React from "react";
import { Label } from "@/shared/ui/label";
import { MultiSelectFilter } from "@/shared/ui/multi-select-filter";
import {
  CLASSIFICATION_GROUPS,
  CREDITS,
  GE_CATEGORIES,
  GRADING_GROUPS,
  LANGUAGES,
  TARGET_GRADES,
  COURSE_DIRECTIONS,
} from "../../constants/course-options";
import type {
  CourseClassification,
  CourseSearchCondition,
  GradingMethod,
  LectureLanguage,
} from "@/shared/types/api";

interface CourseDetailFiltersProps {
  condition: CourseSearchCondition;
  setCondition: React.Dispatch<React.SetStateAction<CourseSearchCondition>>;
}

export function CourseDetailFilters({
  condition,
  setCondition,
}: CourseDetailFiltersProps) {
  // 이수 구분 옵션들 평탄화 (그룹별 라벨 포함)
  const classificationOptions = React.useMemo(() => CLASSIFICATION_GROUPS.flatMap((group) =>
    group.items.map((item) => ({
      label: `${group.label}: ${item}`,
      value: item,
    })),
  ), []);

  // 성적 평가 옵션들 평탄화
  const gradingOptions = React.useMemo(() => GRADING_GROUPS.flatMap((group) =>
    group.items.map((item) => ({
      label: `${group.label}: ${item}`,
      value: item,
    })),
  ), []);

  // 강의 언어 옵션
  const languageOptions = React.useMemo(() => LANGUAGES.map((lang) => ({
    label: lang,
    value: lang,
  })), []);

  // 학점 옵션
  const creditOptions = React.useMemo(() => CREDITS.map((credit) => ({
    label: `${credit}학점`,
    value: credit,
  })), []);

  // 강의 방식 옵션
  const directionOptions = React.useMemo(() => COURSE_DIRECTIONS.map((dir) => ({
    label: dir,
    value: dir,
  })), []);

  // 대상 학년 옵션
  const gradeOptions = React.useMemo(() => TARGET_GRADES.map((grade) => ({
    label: grade === "GRADUATE" ? "대학원" : `${grade}학년`,
    value: grade,
  })), []);

  return (
    <div className="space-y-4">
      {/* 이수 구분 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">이수 구분</Label>
        <MultiSelectFilter
          options={classificationOptions}
          selected={condition.classifications || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              classifications: values as CourseClassification[],
            }))
          }
          placeholder="이수 구분 선택"
        />
      </div>

      {/* 강의 언어 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">강의 언어</Label>
        <MultiSelectFilter
          options={languageOptions}
          selected={condition.lectureLanguages || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              lectureLanguages: values as LectureLanguage[],
            }))
          }
          placeholder="강의 언어 선택"
        />
      </div>

      {/* 성적 평가 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">성적 평가</Label>
        <MultiSelectFilter
          options={gradingOptions}
          selected={condition.gradingMethods || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              gradingMethods: values as GradingMethod[],
            }))
          }
          placeholder="성적 평가 선택"
        />
      </div>

      {/* 학점 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">학점</Label>
        <MultiSelectFilter
          options={creditOptions}
          selected={condition.credits || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              credits: values,
            }))
          }
          placeholder="학점 선택"
        />
      </div>

      {/* 강의 방식 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">강의 방식</Label>
        <MultiSelectFilter
          options={directionOptions}
          selected={condition.statuses || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              statuses: values,
            }))
          }
          placeholder="강의 방식 선택"
        />
      </div>

      {/* 대상 학년 */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">대상 학년</Label>
        <MultiSelectFilter
          options={gradeOptions}
          selected={condition.targetGrades || []}
          onChange={(values) =>
            setCondition((prev) => ({
              ...prev,
              targetGrades: values,
            }))
          }
          placeholder="대상 학년 선택"
        />
      </div>
    </div>
  );
}
