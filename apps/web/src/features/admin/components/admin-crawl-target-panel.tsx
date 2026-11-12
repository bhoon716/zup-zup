"use client";

import { motion } from "framer-motion";
import { Loader2, Play, Save, Settings2 } from "lucide-react";

import { Button } from "@/shared/ui/button";
import { CRAWL_SEMESTER_OPTIONS, getCrawlYearOptions } from "@/features/admin/lib/crawl-semester-options";

interface AdminCrawlTargetPanelProps {

  configuredYear: string;

  configuredSemester: string;

  onConfiguredYearChange: (value: string) => void;

  onConfiguredSemesterChange: (value: string) => void;

  onSaveConfiguredTarget: () => void;

  onRunConfiguredTarget: () => void;

  isConfiguredTargetLoading: boolean;

  isSavingConfiguredTarget: boolean;

  isRunningConfiguredTarget: boolean;

  canSaveConfiguredTarget: boolean;

  runYear: string;

  runSemester: string;

  onRunYearChange: (value: string) => void;

  onRunSemesterChange: (value: string) => void;

  onRunCustomTarget: () => void;

  isRunningCustomTarget: boolean;

  canRunCustomTarget: boolean;
}

export function AdminCrawlTargetPanel({
  configuredYear,
  configuredSemester,
  onConfiguredYearChange,
  onConfiguredSemesterChange,
  onSaveConfiguredTarget,
  onRunConfiguredTarget,
  isConfiguredTargetLoading,
  isSavingConfiguredTarget,
  isRunningConfiguredTarget,
  canSaveConfiguredTarget,
  runYear,
  runSemester,
  onRunYearChange,
  onRunSemesterChange,
  onRunCustomTarget,
  isRunningCustomTarget,
  canRunCustomTarget,
}: AdminCrawlTargetPanelProps) {
  const yearOptions = getCrawlYearOptions();

  return (
    <motion.section
      aria-labelledby="admin-crawl-target-heading"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6"
    >
      <h2 id="admin-crawl-target-heading" className="mb-4 flex items-center gap-2 text-lg font-bold text-slate-900">
        <Settings2 className="size-5 text-primary" />
        크롤링 대상 제어
      </h2>

      <div className="grid gap-3 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 sm:p-4">
          <h3 className="text-sm font-semibold text-slate-900">기본 대상 설정</h3>
          <p className="mb-3 mt-1 text-sm text-slate-600">
            자동 크롤링과 기본 실행에 사용할 연도와 학기를 저장합니다.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <select
              aria-label="기본 대상 연도"
              value={configuredYear}
              onChange={(event) => onConfiguredYearChange(event.target.value)}
              disabled={isConfiguredTargetLoading || isSavingConfiguredTarget}
              className="flex h-9 w-full min-w-0 rounded-md border border-input bg-white px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">연도 선택</option>
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
            <select
              aria-label="기본 대상 학기"
              value={configuredSemester}
              onChange={(event) => onConfiguredSemesterChange(event.target.value)}
              disabled={isConfiguredTargetLoading || isSavingConfiguredTarget}
              className="flex h-9 w-full min-w-0 rounded-md border border-input bg-white px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">학기 선택</option>
              {CRAWL_SEMESTER_OPTIONS.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
            <div className="col-span-2 flex flex-wrap gap-2">
              <Button
                type="button"
                onClick={onSaveConfiguredTarget}
                disabled={isConfiguredTargetLoading || isSavingConfiguredTarget || !canSaveConfiguredTarget}
              >
                {isSavingConfiguredTarget ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
                저장
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={onRunConfiguredTarget}
                disabled={isConfiguredTargetLoading || isRunningConfiguredTarget}
              >
                {isRunningConfiguredTarget ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Play className="size-4" />
                )}
                이 설정으로 실행
              </Button>
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 sm:p-4">
          <h3 className="text-sm font-semibold text-slate-900">특정 대상 즉시 실행</h3>
          <p className="mb-3 mt-1 text-sm text-slate-600">
            선택한 연도와 학기의 강의를 한 번 수집합니다.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <select
              aria-label="즉시 실행 대상 연도"
              value={runYear}
              onChange={(event) => onRunYearChange(event.target.value)}
              disabled={isRunningCustomTarget}
              className="flex h-9 w-full min-w-0 rounded-md border border-input bg-white px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">연도 선택</option>
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
            <select
              aria-label="즉시 실행 대상 학기"
              value={runSemester}
              onChange={(event) => onRunSemesterChange(event.target.value)}
              disabled={isRunningCustomTarget}
              className="flex h-9 w-full min-w-0 rounded-md border border-input bg-white px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">학기 선택</option>
              {CRAWL_SEMESTER_OPTIONS.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
            <Button
              type="button"
              variant="outline"
              className="col-span-2 justify-self-start"
              onClick={onRunCustomTarget}
              disabled={isRunningCustomTarget || !canRunCustomTarget}
            >
              {isRunningCustomTarget ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />}
              지정 값으로 실행
            </Button>
          </div>
        </div>
      </div>
    </motion.section>
  );
}
