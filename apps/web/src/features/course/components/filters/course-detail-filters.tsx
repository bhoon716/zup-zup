"use client";

import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  CLASSIFICATION_GROUPS,
  CREDITS,
  DISCLOSURES,
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
  classificationType: string | undefined;
  setClassificationType: (value: string | undefined) => void;
  gradingType: string | undefined;
  setGradingType: (value: string | undefined) => void;
}

export function CourseDetailFilters({
  condition,
  setCondition,
  classificationType,
  setClassificationType,
  gradingType,
  setGradingType,
}: CourseDetailFiltersProps) {
  // 교양 영역 상세 옵션 계산
  const availableDetails: string[] =
    condition.generalCategory && (GE_CATEGORIES as Record<string, string[]>)[condition.generalCategory]
      ? (GE_CATEGORIES as Record<string, string[]>)[condition.generalCategory]
      : [];

  return (
    <div className="space-y-4">
      {/* 이수 구분 (대분류 & 중분류) */}
      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">이수 구분</Label>
        <Select
          value={classificationType || "all"}
          onValueChange={(value) => {
            if (value === "all") {
              setClassificationType(undefined);
              setCondition((prev) => ({
                ...prev,
                classification: undefined,
                generalCategory: undefined,
                generalDetail: undefined,
              }));
              return;
            }

            const group = CLASSIFICATION_GROUPS.find((g) => g.label === value);
            setClassificationType(value);

            setCondition((prev) => ({
              ...prev,
              classification: group?.value
                ? (group.items[0] as CourseClassification)
                : undefined,
              generalCategory: undefined,
              generalDetail: undefined,
            }));
          }}
        >
          <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
            <SelectValue placeholder="- 선택 -" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">- 선택 -</SelectItem>
            {CLASSIFICATION_GROUPS.map((group) => (
              <SelectItem key={group.label} value={group.label}>
                {group.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* 중분류: 전공/기타 세부 구분 */}
      {classificationType && !["교양", "일반선택"].includes(classificationType) && (
        <div className="mt-2">
          <Select
            value={condition.classification || "all"}
            onValueChange={(value) =>
              setCondition((prev) => ({
                ...prev,
                classification:
                  value === "all" ? undefined : (value as CourseClassification),
              }))
            }
          >
            <SelectTrigger className="h-10 w-full rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 선택 -</SelectItem>
              {CLASSIFICATION_GROUPS.find(
                (g) => g.label === classificationType,
              )?.items.map((item) => (
                <SelectItem key={item} value={item}>
                  {item}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {/* 중분류: 교양 영역 */}
      {classificationType === "교양" && (
        <div className="flex flex-col gap-2 mt-2">
          <Select
            value={condition.generalCategory || "all"}
            onValueChange={(value) =>
              setCondition((prev) => ({
                ...prev,
                generalCategory: value === "all" ? undefined : value,
                generalDetail: undefined,
              }))
            }
          >
            <SelectTrigger className="h-10 w-full rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 선택 -</SelectItem>
              {Object.keys(GE_CATEGORIES).map((category) => (
                <SelectItem key={category} value={category}>
                  {category}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {condition.generalCategory && (
            <Select
              value={condition.generalDetail || "all"}
              onValueChange={(value) =>
                setCondition((prev) => ({
                  ...prev,
                  generalDetail: value === "all" ? undefined : value,
                }))
              }
            >
              <SelectTrigger className="h-10 w-full rounded-xl bg-muted/30 text-sm">
                <SelectValue placeholder="- 선택 -" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">- 선택 -</SelectItem>
                {availableDetails.map((detail) => (
                  <SelectItem key={detail} value={detail}>
                    {detail}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
      )}

      {/* 기타 상세 필터 (그리드 레이아웃) */}
      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">강의 언어</Label>
          <Select
            value={condition.lectureLanguage || "all"}
            onValueChange={(value) =>
              setCondition((prev) => ({
                ...prev,
                lectureLanguage:
                  value === "all" ? undefined : (value as LectureLanguage),
              }))
            }
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 선택 -</SelectItem>
              {LANGUAGES.map((language) => (
                <SelectItem key={language} value={language}>
                  {language}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5 ">
          <Label className="text-[11px] font-bold text-muted-foreground">성적 평가</Label>
          <div className="flex flex-col gap-2">
            <Select
              value={gradingType || "all"}
              onValueChange={(value) => {
                if (value === "all") {
                  setGradingType(undefined);
                  setCondition((prev) => ({
                    ...prev,
                    gradingMethod: undefined,
                  }));
                  return;
                }
                setGradingType(value);
                setCondition((prev) => ({
                  ...prev,
                  gradingMethod: undefined,
                }));
              }}
            >
              <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                <SelectValue placeholder="- 선택 -" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">- 선택 -</SelectItem>
                {GRADING_GROUPS.map((group) => (
                  <SelectItem key={group.label} value={group.label}>
                    {group.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {gradingType && (
              <Select
                value={condition.gradingMethod || "all"}
                onValueChange={(value) =>
                  setCondition((prev) => ({
                    ...prev,
                    gradingMethod:
                      value === "all" ? undefined : (value as GradingMethod),
                  }))
                }
              >
                <SelectTrigger className="h-10 w-full rounded-xl bg-muted/30 text-sm">
                  <SelectValue placeholder="- 선택 -" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">- 선택 -</SelectItem>
                  {GRADING_GROUPS.find((g) => g.label === gradingType)?.items.map(
                    (item) => (
                      <SelectItem key={item} value={item}>
                        {item}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">학점</Label>
          <Select
            value={condition.credits || "all"}
            onValueChange={(value) =>
              setCondition((prev) => ({
                ...prev,
                credits: value === "all" ? undefined : value,
              }))
            }
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 선택 -</SelectItem>
              {CREDITS.map((credit) => (
                <SelectItem key={credit} value={credit}>
                  {credit}학점
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label className="text-[11px] font-bold text-muted-foreground">공개 여부</Label>
          <Select
            value={condition.disclosure || "all"}
            onValueChange={(value) =>
              setCondition((prev) => ({
                ...prev,
                disclosure: value === "all" ? undefined : value,
              }))
            }
          >
            <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
              <SelectValue placeholder="- 선택 -" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">- 선택 -</SelectItem>
              {DISCLOSURES.map((status) => (
                <SelectItem key={status} value={status}>
                  {status}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">강의 방식</Label>
        <Select
          value={condition.courseDirection || "all"}
          onValueChange={(value) =>
            setCondition((prev) => ({
              ...prev,
              courseDirection: value === "all" ? undefined : value,
            }))
          }
        >
          <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
            <SelectValue placeholder="- 선택 -" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">- 선택 -</SelectItem>
            {COURSE_DIRECTIONS.map((direction) => (
              <SelectItem key={direction} value={direction}>
                {direction}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-1.5">
        <Label className="text-[11px] font-bold text-muted-foreground">대상 학년</Label>
        <Select
          value={condition.targetGrade || "all"}
          onValueChange={(value) =>
            setCondition((prev) => ({
              ...prev,
              targetGrade: value === "all" ? undefined : value,
            }))
          }
        >
          <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
            <SelectValue placeholder="- 선택 -" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">- 선택 -</SelectItem>
            {TARGET_GRADES.map((grade) => (
              <SelectItem key={grade} value={grade}>
                {grade === "GRADUATE" ? "대학원" : `${grade}학년`}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
