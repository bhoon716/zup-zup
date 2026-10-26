"use client";

import React from "react";
import { Label } from "@/shared/ui/label";
import { MultiSelectFilter } from "@/shared/ui/multi-select-filter";
import {
  CLASSIFICATION_GROUPS,
  CREDITS,
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

  const classificationOptions = React.useMemo(() => CLASSIFICATION_GROUPS.flatMap((group) =>
    group.items.map((item) => ({
      label: `${group.label}: ${item}`,
      value: item,
    })),
  ), []);


  const gradingOptions = React.useMemo(() =>
    GRADING_GROUPS.flatMap((group) =>
      group.items.map((item) => ({
        label: item,
        value: item,
      })),
    ), []);


  const languageOptions = React.useMemo(() => LANGUAGES.map((lang) => ({
    label: lang,
    value: lang,
  })), []);


  const creditOptions = React.useMemo(() => CREDITS.map((credit) => ({
    label: `${credit}학점`,
    value: credit,
  })), []);


  const directionOptions = React.useMemo(() => COURSE_DIRECTIONS.map((dir) => ({
    label: dir,
    value: dir,
  })), []);


  const gradeOptions = React.useMemo(() => TARGET_GRADES.map((grade) => ({
    label: grade === "GRADUATE" ? "대학원" : `${grade}학년`,
    value: grade,
  })), []);

  return (
    <div className="space-y-4">
      {           }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">이수 구분</Label>
        <MultiSelectFilter
          idBase="course-detail-classification"
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

      {           }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">강의 언어</Label>
        <MultiSelectFilter
          idBase="course-detail-language"
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

      {           }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">성적 평가</Label>
        <MultiSelectFilter
          idBase="course-detail-grading"
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

      {        }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">학점</Label>
        <MultiSelectFilter
          idBase="course-detail-credit"
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

      {           }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">강의 방식</Label>
        <MultiSelectFilter
          idBase="course-detail-direction"
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

      {           }
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">대상 학년</Label>
        <MultiSelectFilter
          idBase="course-detail-grade"
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
