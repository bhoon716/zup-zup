"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronUp, RotateCcw } from "lucide-react";
import { useState } from "react";
import type { CourseSearchCondition } from "@/types/api";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
}

export function CourseSearchBar({ onSearch }: CourseSearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [condition, setCondition] = useState<CourseSearchCondition>({});

  const handleSearch = () => {
    onSearch({
      ...condition,
      keyword: keyword // keyword acts as the consolidated search term
    });
  };

  const handleReset = () => {
    setKeyword("");
    setCondition({});
    onSearch({});
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="space-y-4">
      {/* 1. Basic Search Conditions (Visible by default) */}
      <div className="space-y-4 p-4 border rounded-md bg-card shadow-sm">
        <h3 className="text-sm font-semibold mb-2">기본 검색 조건</h3>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">연도</label>
            <Input
              placeholder="2026"
              value={condition.academicYear || ''}
              onChange={(e) => setCondition({ ...condition, academicYear: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">학기</label>
            <Input
              placeholder="1 / 2 ..."
              value={condition.semester || ''}
              onChange={(e) => setCondition({ ...condition, semester: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">검색어 (과목/교수/코드)</label>
            <Input
              type="text"
              placeholder="검색어 입력..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
            />
          </div>
           <div className="space-y-2">
            <label className="text-sm font-medium">교수명 (상세)</label>
            <Input
              placeholder="교수명"
              value={condition.professor || ''}
              onChange={(e) => setCondition({ ...condition, professor: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">이수구분</label>
            <Input
              placeholder="전공선택/교양..."
              value={condition.classification || ''}
              onChange={(e) => setCondition({ ...condition, classification: e.target.value })}
            />
          </div>
          <div className="flex items-center space-x-2 pt-8">
            <label className="text-sm font-medium flex items-center gap-2 cursor-pointer">
              <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-gray-300"
                  checked={condition.isAvailableOnly || false}
                  onChange={(e) => setCondition({ ...condition, isAvailableOnly: e.target.checked })}
              />
              <span>잔여석 있음</span>
            </label>
          </div>
        </div>

        <div className="flex justify-between items-center pt-2">
            <Collapsible open={isOpen} onOpenChange={setIsOpen}>
              <CollapsibleTrigger asChild>
                <Button variant="ghost" className="gap-2 text-sm">
                  상세 조건 {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                </Button>
              </CollapsibleTrigger>
            </Collapsible>

            <div className="flex gap-2">
                <Button variant="ghost" onClick={handleReset} size="sm" className="gap-2 text-muted-foreground hover:text-foreground">
                    <RotateCcw className="w-3 h-3" />
                    초기화
                </Button>
                <Button onClick={handleSearch}>
                    검색
                </Button>
            </div>
        </div>
      </div>

      {/* 2. Detailed Search Conditions (Collapsible) */}
      <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <CollapsibleContent className="p-4 border rounded-md bg-card space-y-4 shadow-sm mt-2">
          <h3 className="text-sm font-semibold mb-2">상세 검색 조건</h3>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">과목명 (상세)</label>
              <Input
                placeholder="과목명"
                value={condition.name || ''}
                onChange={(e) => setCondition({ ...condition, name: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">학과</label>
              <Input
                placeholder="학과"
                value={condition.department || ''}
                onChange={(e) => setCondition({ ...condition, department: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">성적평가</label>
              <Input
                placeholder="절대/상대"
                value={condition.gradingMethod || ''}
                onChange={(e) => setCondition({ ...condition, gradingMethod: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">수업방식</label>
              <Input
                placeholder="대면/비대면"
                value={condition.lectureType || ''}
                onChange={(e) => setCondition({ ...condition, lectureType: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">강의언어</label>
              <Input
                placeholder="한국어/영어"
                value={condition.lectureLanguage || ''}
                onChange={(e) => setCondition({ ...condition, lectureLanguage: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">요일</label>
              <select
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={condition.dayOfWeek || ''}
                onChange={(e) => setCondition({ ...condition, dayOfWeek: e.target.value })}
              >
                <option value="">전체</option>
                <option value="월">월요일</option>
                <option value="화">화요일</option>
                <option value="수">수요일</option>
                <option value="목">목요일</option>
                <option value="금">금요일</option>
                <option value="토">토요일</option>
              </select>
            </div>
             <div className="space-y-2">
              <label className="text-sm font-medium">교시</label>
               <Input
                placeholder="1-A, 2-B..."
                value={condition.period || ''}
                onChange={(e) => setCondition({ ...condition, period: e.target.value })}
              />
            </div>
          </div>
        </CollapsibleContent>
      </Collapsible>
    </div>
  );
}
