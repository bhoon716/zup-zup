"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronUp, RotateCcw } from "lucide-react";
import { useState } from "react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Checkbox } from "@/components/ui/checkbox";
import type { 
  CourseSearchCondition, 
  CourseClassification, 
  GradingMethod, 
  LectureType, 
  LectureLanguage, 
  CourseDayOfWeek, 
  ClassPeriod 
} from "@/types/api";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
}

const CLASSIFICATIONS: CourseClassification[] = ['계열공통', '교양', '교직(대)', '교직', '군사학', '기초필수', '선수', '일반선택', '전공', '전공선택', '전공필수'];
const GRADING_METHODS: GradingMethod[] = ['Pass/Fail', '기타(법전원)', '상대평가Ⅰ', '상대평가Ⅱ', '상대평가Ⅲ', '절대평가'];
const LECTURE_TYPES: LectureType[] = ['대면중심수업(70%미만 온라인)', '비대면수업', '혼합수업'];
const LANGUAGES: LectureLanguage[] = ['한국어', '영어', '독일어', '스페인어', '일본어', '중국어', '프랑스어'];
const DAYS: CourseDayOfWeek[] = ['월', '화', '수', '목', '금', '토', '일'];
const PERIOD_NUMS = Array.from({ length: 16 }, (_, i) => i.toString()); // 0 ~ 15

export function CourseSearchBar({ onSearch }: CourseSearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [condition, setCondition] = useState<CourseSearchCondition>({});

  const handleSearch = () => {
    onSearch({
      ...condition,
      keyword: keyword.trim() || undefined
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
      <div className="p-6 border rounded-xl bg-card/50 backdrop-blur-sm shadow-xl border-white/10">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
            강좌 상세 검색
          </h3>
          <div className="flex items-center gap-4">
            <div className="flex items-center space-x-2">
              <Checkbox 
                id="isAvailableOnly" 
                checked={condition.isAvailableOnly || false}
                onCheckedChange={(checked) => setCondition({ ...condition, isAvailableOnly: !!checked })}
              />
              <label 
                htmlFor="isAvailableOnly"
                className="text-sm font-medium leading-none cursor-pointer select-none"
              >
                잔여석 있음
              </label>
            </div>
            <Button variant="outline" onClick={handleReset} size="sm" className="gap-2 h-9 px-4 rounded-full border-primary/20 hover:border-primary/50 transition-all duration-300">
              <RotateCcw className="w-3.5 h-3.5" />
              초기화
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">통합 검색어</label>
            <div className="relative group">
              <Input
                type="text"
                placeholder="과목명, 교수명, 과목코드..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={handleKeyDown}
                className="bg-background/50 border-white/5 focus-visible:ring-primary/30 h-10 transition-all duration-300 group-hover:border-primary/20"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">연도 / 학기</label>
            <div className="flex gap-2">
              <Input
                placeholder="연도"
                value={condition.academicYear || ''}
                onChange={(e) => setCondition({ ...condition, academicYear: e.target.value })}
                className="bg-background/50 border-white/5 focus-visible:ring-primary/30 h-10 w-24"
              />
              <Select 
                value={condition.semester || ''} 
                onValueChange={(val) => setCondition({ ...condition, semester: val })}
              >
                <SelectTrigger className="bg-background/50 border-white/5 focus:ring-primary/30 h-10">
                  <SelectValue placeholder="학기" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">1학기</SelectItem>
                  <SelectItem value="2">2학기</SelectItem>
                  <SelectItem value="U010200021">여름학기</SelectItem>
                  <SelectItem value="U010200022">겨울학기</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">이수구분</label>
            <Select 
              value={condition.classification || ''} 
              onValueChange={(val) => setCondition({ ...condition, classification: val as CourseClassification })}
            >
              <SelectTrigger className="bg-background/50 border-white/5 focus:ring-primary/30 h-10">
                <SelectValue placeholder="전체" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체</SelectItem>
                {CLASSIFICATIONS.map(c => <SelectItem key={c} value={c}>{c}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-end">
            <Button onClick={handleSearch} className="w-full h-10 bg-primary hover:bg-primary/90 shadow-lg shadow-primary/20 rounded-lg transition-all duration-300 font-bold">
              강좌 검색
            </Button>
          </div>
        </div>

        <Collapsible open={isOpen} onOpenChange={setIsOpen} className="mt-6">
          <div className="flex justify-center -mb-3 relative z-10">
            <CollapsibleTrigger asChild>
              <Button variant="ghost" size="sm" className="gap-2 h-7 px-4 rounded-full bg-background border border-white/10 hover:bg-muted transition-all duration-300 shadow-sm">
                {isOpen ? '상세 조건 접기' : '상세 조건 펼치기'} 
                {isOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              </Button>
            </CollapsibleTrigger>
          </div>

          <CollapsibleContent className="pt-8 pb-4 px-2 space-y-6 border-t border-white/5">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">성적평가방식</label>
                <Select 
                  value={condition.gradingMethod || ''} 
                  onValueChange={(val) => setCondition({ ...condition, gradingMethod: val as GradingMethod })}
                >
                  <SelectTrigger className="bg-background/50 border-white/5 h-10">
                    <SelectValue placeholder="전체" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체</SelectItem>
                    {GRADING_METHODS.map(g => <SelectItem key={g} value={g}>{g}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">수업방식</label>
                <Select 
                  value={condition.lectureType || ''} 
                  onValueChange={(val) => setCondition({ ...condition, lectureType: val as LectureType })}
                >
                  <SelectTrigger className="bg-background/50 border-white/5 h-10">
                    <SelectValue placeholder="전체" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체</SelectItem>
                    {LECTURE_TYPES.map(l => <SelectItem key={l} value={l}>{l}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">강의언어</label>
                <Select 
                  value={condition.lectureLanguage || ''} 
                  onValueChange={(val) => setCondition({ ...condition, lectureLanguage: val as LectureLanguage })}
                >
                  <SelectTrigger className="bg-background/50 border-white/5 h-10">
                    <SelectValue placeholder="전체" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체</SelectItem>
                    {LANGUAGES.map(l => <SelectItem key={l} value={l}>{l}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">강의 요일 / 교시</label>
                <div className="flex gap-2">
                  <Select 
                    value={condition.dayOfWeek || ''} 
                    onValueChange={(val) => setCondition({ ...condition, dayOfWeek: val as CourseDayOfWeek })}
                  >
                    <SelectTrigger className="bg-background/50 border-white/5 h-10 flex-1">
                      <SelectValue placeholder="요일" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">요일(전체)</SelectItem>
                      {DAYS.map(d => <SelectItem key={d} value={d}>{d}요일</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <Select 
                    value={condition.period || ''} 
                    onValueChange={(val) => setCondition({ ...condition, period: val as ClassPeriod })}
                  >
                    <SelectTrigger className="bg-background/50 border-white/5 h-10 w-24">
                      <SelectValue placeholder="교시" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">교시(전체)</SelectItem>
                      {PERIOD_NUMS.map(n => <SelectItem key={n} value={`${n}-A`}>{n}-A</SelectItem>)}
                      {PERIOD_NUMS.map(n => <SelectItem key={n} value={`${n}-B`}>{n}-B</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">학과 / 단과대</label>
                <Input
                  placeholder="컴퓨터공학..."
                  value={condition.department || ''}
                  onChange={(e) => setCondition({ ...condition, department: e.target.value })}
                  className="bg-background/50 border-white/5 h-10"
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">과목명 (정밀)</label>
                <Input
                  placeholder="특정 과목명 입력"
                  value={condition.name || ''}
                  onChange={(e) => setCondition({ ...condition, name: e.target.value })}
                  className="bg-background/50 border-white/5 h-10"
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">교수명 (정밀)</label>
                <Input
                  placeholder="특정 교수명 입력"
                  value={condition.professor || ''}
                  onChange={(e) => setCondition({ ...condition, professor: e.target.value })}
                  className="bg-background/50 border-white/5 h-10"
                />
              </div>

               <div className="space-y-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/80">과목코드 (정밀)</label>
                <Input
                  placeholder="CLTR.0031"
                  value={condition.subjectCode || ''}
                  onChange={(e) => setCondition({ ...condition, subjectCode: e.target.value })}
                  className="bg-background/50 border-white/5 h-10"
                />
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>
      </div>
    </div>
  );
}
