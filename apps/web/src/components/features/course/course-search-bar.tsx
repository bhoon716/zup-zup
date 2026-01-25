"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ChevronDown, ChevronUp, RotateCcw, Search } from "lucide-react";
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
import { TimeTableSelector } from "./time-table-selector";
import type {
  CourseSearchCondition,
  CourseClassification,
  GradingMethod,
  LectureLanguage,
  ScheduleCondition
} from "@/types/api";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
  isLoading?: boolean;
}

const CLASSIFICATIONS: CourseClassification[] = ['계열공통', '교양', '교직(대)', '교직', '군사학', '기초필수', '선수', '일반선택', '전공', '전공선택', '전공필수'];
const GRADING_METHODS: GradingMethod[] = ['Pass/Fail', '기타(법전원)', '상대평가Ⅰ', '상대평가Ⅱ', '상대평가Ⅲ', '절대평가'];
const LANGUAGES: LectureLanguage[] = ['한국어', '영어', '독일어', '스페인어', '일본어', '중국어', '프랑스어'];
const CREDITS = ['0.5', '1', '2', '3'];
const LECTURE_HOURS = [...Array.from({ length: 9 }, (_, i) => (i + 1).toString()), '10+'];

import { useEffect } from "react";
import * as courseApi from "@/lib/api/course";
import type { CourseCategoryResponse } from "@/types/api";

export function CourseSearchBar({ onSearch, isLoading }: CourseSearchBarProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [categories, setCategories] = useState<CourseCategoryResponse[]>([]);
  const [condition, setCondition] = useState<CourseSearchCondition>({
    academicYear: '2026',
    semester: 'U211600010'
  });

  useEffect(() => {
    courseApi.getCourseCategories().then(res => {
      setCategories(res.data);
    });
  }, []);

  const selectedCategory = categories.find(c => c.category === condition.generalCategory);
  const availableDetails = selectedCategory ? selectedCategory.details : [];

  const YEARS = ["2026", "2025", "2024"];
  const SEMESTERS = [
    { label: "1학기", value: "U211600010", short: "1학기" },
    { label: "계절학기(하기)", value: "U010200021", short: "계절(하)" },
    { label: "2학기", value: "U211600020", short: "2학기" },
    { label: "계절학기(동기)", value: "U010200022", short: "계절(동)" },
    { label: "특별학기(여름)", value: "S1", short: "특(여)" },
    { label: "특별학기(겨울)", value: "S2", short: "특(겨)" },
    { label: "특별학기(신입생)", value: "S3", short: "특(신)" },
    { label: "특별학기(SW)", value: "S4", short: "특(SW)" },
  ];

  const handleSearch = () => {
    onSearch(condition);
    setIsOpen(false);
  };

  const handleReset = () => {
    setCondition({
      academicYear: '2026',
      semester: 'U211600010'
    });
    onSearch({
      academicYear: '2026',
      semester: 'U211600010'
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleSchedulesChange = (selected: ScheduleCondition[]) => {
    setCondition({ ...condition, selectedSchedules: selected });
  };

  return (
    <div className="space-y-2">
      <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <div className="p-4 border rounded-2xl bg-card/40 backdrop-blur-xl shadow-2xl border-white/10 ring-1 ring-white/5">
          {/* Compressed Single Line Header & Primary Controls */}
          <div className="flex flex-col lg:flex-row items-center gap-3">
            {/* Section 1: All Primary Fields in One Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-2 flex-grow w-full">
              {/* Year (Compact) */}
              <div className="lg:col-span-1">
                <Select 
                  value={condition.academicYear || '2026'} 
                  onValueChange={(val) => setCondition({ ...condition, academicYear: val })}
                >
                  <SelectTrigger className="bg-background/40 border-white/10 h-10 text-[11px] font-bold rounded-xl transition-all hover:bg-background/60">
                    <SelectValue placeholder="연도" />
                  </SelectTrigger>
                  <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                    {YEARS.map(y => <SelectItem key={y} value={y}>{y}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>

              {/* Semester (Compact) */}
              <div className="lg:col-span-1">
                <Select 
                  value={condition.semester || '1'} 
                  onValueChange={(val) => setCondition({ ...condition, semester: val })}
                >
                  <SelectTrigger className="bg-background/40 border-white/10 h-10 text-[11px] font-bold rounded-xl transition-all hover:bg-background/60">
                    <SelectValue placeholder="학기" />
                  </SelectTrigger>
                  <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                    {SEMESTERS.map(s => <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>

              {/* Name & Professor */}
              <div className="lg:col-span-4">
                <Input
                  type="text"
                  placeholder="과목명"
                  value={condition.name || ''}
                  onChange={(e) => setCondition({ ...condition, name: e.target.value })}
                  onKeyDown={handleKeyDown}
                  className="bg-background/40 border-white/10 focus-visible:ring-primary/20 h-10 text-xs font-medium rounded-xl transition-all hover:bg-background/60"
                />
              </div>

              <div className="lg:col-span-3">
                <Input
                  type="text"
                  placeholder="교수명"
                  value={condition.professor || ''}
                  onChange={(e) => setCondition({ ...condition, professor: e.target.value })}
                  onKeyDown={handleKeyDown}
                  className="bg-background/40 border-white/10 focus-visible:ring-primary/20 h-10 text-xs font-medium rounded-xl transition-all hover:bg-background/60"
                />
              </div>

              {/* Availability Checkbox (Integrated) - Compact */}
              <div className="lg:col-span-1 flex items-center justify-center bg-background/30 border border-dashed border-white/10 rounded-xl px-1 group cursor-pointer hover:bg-background/50 transition-all" onClick={() => setCondition({ ...condition, isAvailableOnly: !condition.isAvailableOnly })}>
                <Checkbox 
                  id="isAvailableOnly" 
                  checked={condition.isAvailableOnly || false}
                  onCheckedChange={(checked) => setCondition({ ...condition, isAvailableOnly: !!checked })}
                  className="data-[state=checked]:bg-primary data-[state=checked]:border-primary w-3.5 h-3.5"
                />
                <label 
                  htmlFor="isAvailableOnly"
                  className="text-[10px] font-bold text-muted-foreground group-hover:text-foreground cursor-pointer select-none transition-colors ml-1.5 truncate"
                >
                  잔여석 존재
                </label>
              </div>

              {/* Action Buttons (Integrated) - Compact */}
              <div className="lg:col-span-2 flex items-center gap-1.5">
                <Button 
                  onClick={handleSearch} 
                  disabled={isLoading}
                  className="flex-1 h-10 bg-primary hover:bg-primary/90 text-primary-foreground font-black rounded-xl transition-all shadow-lg shadow-primary/20 disabled:opacity-70"
                >
                  {isLoading ? (
                     <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <Search className="w-4 h-4" />
                  )}
                </Button>
                <Button 
                  variant="outline" 
                  onClick={handleReset} 
                  className="w-10 h-10 p-0 rounded-xl border-white/10 hover:bg-white/5 transition-all flex-shrink-0"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                </Button>
              </div>
            </div>
          </div>

          <div className="overflow-hidden">
            <CollapsibleContent className="data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up">
              <div className="flex flex-col lg:flex-row gap-8 mt-4 pt-4 border-t border-white/5">
                {/* Left Column: Timetable */}
                <div className="flex-shrink-0 space-y-3">
                  <div className="flex items-center gap-2 mb-1">
                    <div className="w-1 h-3 bg-primary/60 rounded-full"></div>
                    <label className="text-[10px] font-black uppercase tracking-widest text-primary/80">
                      수강 가능 시간표
                    </label>
                  </div>
                  <TimeTableSelector 
                    selected={condition.selectedSchedules || []}
                    onChange={handleSchedulesChange}
                  />
                </div>

                {/* Right Column: Advanced Filters */}
                <div className="flex-grow space-y-6">
                  {/* Group 1: Academic Info */}
                  <div className="p-4 rounded-2xl bg-background/20 border border-white/5 space-y-4">
                    <div className="flex items-center gap-2 mb-1">
                      <div className="w-1 h-3 bg-primary/40 rounded-full"></div>
                      <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/60">Academic Info</span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-5 gap-5">
                      <div className="space-y-2 col-span-1 md:col-span-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">SUBJECT CODE</label>
                        <Input
                          placeholder="과목코드 (예: CLTR.0031)"
                          value={condition.subjectCode || ''}
                          onChange={(e) => setCondition({ ...condition, subjectCode: e.target.value })}
                          className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl placeholder:text-muted-foreground/40"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">CLASSIFICATION</label>
                        <Select 
                          value={condition.classification || ''} 
                          onValueChange={(val) => setCondition({ ...condition, classification: val === 'all' ? undefined : val as CourseClassification })}
                        >
                          <SelectTrigger className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl">
                            <SelectValue placeholder="이수구분" />
                          </SelectTrigger>
                          <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                            <SelectItem value="all">전체</SelectItem>
                            {CLASSIFICATIONS.map(c => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">CREDITS</label>
                        <Select 
                          value={condition.credits || ''} 
                          onValueChange={(val) => setCondition({ ...condition, credits: val === 'all' ? undefined : val })}
                        >
                          <SelectTrigger className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl">
                            <SelectValue placeholder="학점" />
                          </SelectTrigger>
                          <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                            <SelectItem value="all">전체</SelectItem>
                            {CREDITS.map(c => <SelectItem key={c} value={c}>{c}학점</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">HOURS</label>
                        <Select 
                          value={condition.minLectureHours ? '10+' : (condition.lectureHours?.toString() || '')} 
                          onValueChange={(val) => {
                            if (val === 'all') {
                              setCondition({ ...condition, lectureHours: undefined, minLectureHours: undefined });
                            } else if (val === '10+') {
                              setCondition({ ...condition, lectureHours: undefined, minLectureHours: 10 });
                            } else {
                              setCondition({ ...condition, lectureHours: parseInt(val), minLectureHours: undefined });
                            }
                          }}
                        >
                          <SelectTrigger className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl">
                            <SelectValue placeholder="시수" />
                          </SelectTrigger>
                          <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                            <SelectItem value="all">전체</SelectItem>
                            {LECTURE_HOURS.map(h => <SelectItem key={h} value={h}>{h === '10+' ? '10시간 이상' : `${h}시간`}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  </div>

                  {/* Group 2: Lecture Info */}
                  <div className="p-4 rounded-2xl bg-background/20 border border-white/5 space-y-4">
                    <div className="flex items-center gap-2 mb-1">
                      <div className="w-1 h-3 bg-primary/40 rounded-full"></div>
                      <span className="text-[10px] font-black uppercase tracking-[0.2em] text-muted-foreground/60">Lecture Details</span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">DEPARTMENT</label>
                        <Input
                          placeholder="학과 / 단과대명"
                          value={condition.department || ''}
                          onChange={(e) => setCondition({ ...condition, department: e.target.value })}
                          className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">GRADING METHOD</label>
                        <Select 
                          value={condition.gradingMethod || ''} 
                          onValueChange={(val) => setCondition({ ...condition, gradingMethod: val === 'all' ? undefined : val as GradingMethod })}
                        >
                          <SelectTrigger className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl">
                            <SelectValue placeholder="성적평가방식" />
                          </SelectTrigger>
                          <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                            <SelectItem value="all">전체</SelectItem>
                            {GRADING_METHODS.map(g => <SelectItem key={g} value={g}>{g}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <label className="text-[9px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">LANGUAGE</label>
                        <Select 
                          value={condition.lectureLanguage || ''} 
                          onValueChange={(val) => setCondition({ ...condition, lectureLanguage: val === 'all' ? undefined : val as LectureLanguage })}
                        >
                          <SelectTrigger className="bg-background/30 border-white/5 h-10 text-[11px] font-medium rounded-xl">
                            <SelectValue placeholder="강의언어" />
                          </SelectTrigger>
                          <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                            <SelectItem value="all">전체</SelectItem>
                            {LANGUAGES.map(l => <SelectItem key={l} value={l}>{l}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  </div>

                  {/* Group 3: General Education (Conditional) */}
                  {condition.classification === '교양' && (
                    <div className="p-4 rounded-2xl bg-primary/5 border border-primary/20 space-y-4 animate-in fade-in slide-in-from-top-2 duration-300">
                      <div className="flex items-center gap-2 mb-1">
                        <div className="w-1 h-3 bg-primary/60 rounded-full"></div>
                        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/80">General Education Details</span>
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                        <div className="space-y-2">
                          <label className="text-[9px] font-black uppercase tracking-widest text-primary/40 ml-1">CATEGORY (교양영역)</label>
                          <Select 
                            value={condition.generalCategory || ''} 
                            onValueChange={(val) => setCondition({ 
                              ...condition, 
                              generalCategory: val === 'all' ? undefined : val,
                              generalDetail: undefined 
                            })}
                          >
                            <SelectTrigger className="bg-background/30 border-primary/10 h-10 text-[11px] font-medium rounded-xl">
                              <SelectValue placeholder="교양영역 전체" />
                            </SelectTrigger>
                            <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                              <SelectItem value="all">전체</SelectItem>
                              {categories.map(c => <SelectItem key={c.category} value={c.category}>{c.category}</SelectItem>)}
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="space-y-2">
                          <label className="text-[9px] font-black uppercase tracking-widest text-primary/40 ml-1">DETAIL (상세영역)</label>
                          <Select 
                            value={condition.generalDetail || ''} 
                            onValueChange={(val) => setCondition({ ...condition, generalDetail: val === 'all' ? undefined : val })}
                            disabled={!condition.generalCategory}
                          >
                            <SelectTrigger className="bg-background/30 border-primary/10 h-10 text-[11px] font-medium rounded-xl disabled:opacity-50">
                              <SelectValue placeholder={condition.generalCategory ? "상세영역 선택" : "영역을 먼저 선택하세요"} />
                            </SelectTrigger>
                            <SelectContent className="bg-card/90 backdrop-blur-xl border-white/10">
                              <SelectItem value="all">전체</SelectItem>
                              {availableDetails.map(d => <SelectItem key={d} value={d}>{d}</SelectItem>)}
                            </SelectContent>
                          </Select>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </CollapsibleContent>
          </div>
        </div>

        {/* Repositioned Expand Toggle (Bottom Center) */}
        <div className="flex justify-center -mt-4 relative z-10">
          <CollapsibleTrigger asChild>
            <Button 
              variant="ghost" 
              size="sm" 
              className="gap-2 h-7 px-8 rounded-full bg-card border border-white/10 hover:bg-accent transition-all shadow-sm text-[9px] font-black uppercase tracking-wider"
            >
              {isOpen ? 'SIMPLE' : 'EXPAND'} 
              {isOpen ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
            </Button>
          </CollapsibleTrigger>
        </div>
      </Collapsible>
    </div>
  );
}
