"use client";

import { motion } from "framer-motion";
import { Loader2, Play, Save, Send, Settings2 } from "lucide-react";

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

  onSendTest: () => void;

  isSendingTest: boolean;
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
  onSendTest,
  isSendingTest,
}: AdminCrawlTargetPanelProps) {
  const yearOptions = getCrawlYearOptions();

  return (
    <motion.section
      aria-labelledby="admin-crawl-target-heading"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl shadow-slate-200/40 lg:rounded-[2.5rem] lg:p-8"
    >
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Settings2 className="h-5 w-5 text-primary" />
          <h2 id="admin-crawl-target-heading" className="text-xl font-black tracking-tight text-slate-900">크롤링 대상 제어</h2>
        </div>
        <Button
          type="button"
          variant="outline"
          onClick={onSendTest}
          disabled={isSendingTest}
          className="h-9 rounded-xl px-4 text-xs font-bold"
        >
          {isSendingTest ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          자가 알림 테스트
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
          <h3 className="mb-3 text-sm font-black text-slate-900">기본 대상 설정</h3>
          <p className="mb-4 text-xs font-medium text-slate-500">
            스케줄러 및 기본 크롤링 버튼에서 사용할 년도/학기를 저장합니다.
          </p>
          <div className="space-y-3">
            <select
              value={configuredYear}
              onChange={(event) => onConfiguredYearChange(event.target.value)}
              disabled={isConfiguredTargetLoading || isSavingConfiguredTarget}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">년도 선택</option>
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
            <select
              value={configuredSemester}
              onChange={(event) => onConfiguredSemesterChange(event.target.value)}
              disabled={isConfiguredTargetLoading || isSavingConfiguredTarget}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">학기 선택</option>
              {CRAWL_SEMESTER_OPTIONS.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                onClick={onSaveConfiguredTarget}
                disabled={isConfiguredTargetLoading || isSavingConfiguredTarget || !canSaveConfiguredTarget}
                className="h-9 rounded-xl bg-primary px-4 text-xs font-bold text-white hover:bg-primary-dark"
              >
                {isSavingConfiguredTarget ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Save className="mr-1 h-4 w-4" />}
                저장
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={onRunConfiguredTarget}
                disabled={isConfiguredTargetLoading || isRunningConfiguredTarget}
                className="h-9 rounded-xl px-4 text-xs font-bold"
              >
                {isRunningConfiguredTarget ? (
                  <Loader2 className="mr-1 h-4 w-4 animate-spin" />
                ) : (
                  <Play className="mr-1 h-4 w-4" />
                )}
                이 설정으로 실행
              </Button>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
          <h3 className="mb-3 text-sm font-black text-slate-900">특정 대상 즉시 실행</h3>
          <p className="mb-4 text-xs font-medium text-slate-500">
            저장과 무관하게 1회성으로 원하는 년도/학기를 즉시 크롤링합니다.
          </p>
          <div className="space-y-3">
            <select
              value={runYear}
              onChange={(event) => onRunYearChange(event.target.value)}
              disabled={isRunningCustomTarget}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">년도 선택</option>
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
            <select
              value={runSemester}
              onChange={(event) => onRunSemesterChange(event.target.value)}
              disabled={isRunningCustomTarget}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
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
              onClick={onRunCustomTarget}
              disabled={isRunningCustomTarget || !canRunCustomTarget}
              className="h-9 rounded-xl px-4 text-xs font-bold"
            >
              {isRunningCustomTarget ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Play className="mr-1 h-4 w-4" />}
              지정 값으로 실행
            </Button>
          </div>
        </div>
      </div>
    </motion.section>
  );
}
