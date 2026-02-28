"use client";

import { motion } from "framer-motion";
import { Loader2, Play, Save, Settings2 } from "lucide-react";

import { Button } from "@/shared/ui/button";
import { CRAWL_SEMESTER_OPTIONS, getCrawlYearOptions } from "@/features/admin/lib/crawl-semester-options";

/**
 * 관리자 크롤링 타겟 패널의 Props 인터페이스입니다.
 */
interface AdminCrawlTargetPanelProps {
  /** 현재 DB에 저장되어 스케줄러가 사용하는 년도 */
  configuredYear: string;
  /** 현재 DB에 저장되어 스케줄러가 사용하는 학기 코드 */
  configuredSemester: string;
  /** 기본 년도 입력 변경 핸들러 */
  onConfiguredYearChange: (value: string) => void;
  /** 기본 학기 코드 입력 변경 핸들러 */
  onConfiguredSemesterChange: (value: string) => void;
  /** 수정된 기본 설정을 서버에 저장합니다. */
  onSaveConfiguredTarget: () => void;
  /** 현재 저장된 기본 설정으로 크롤링을 실행합니다. */
  onRunConfiguredTarget: () => void;
  /** 설정 정보 로딩 상태 */
  isConfiguredTargetLoading: boolean;
  /** 설정 저장 진행 상태 */
  isSavingConfiguredTarget: boolean;
  /** 저장된 값으로 실행 진행 상태 */
  isRunningConfiguredTarget: boolean;
  /** 저장 가능 여부 (유효성 검증 결과) */
  canSaveConfiguredTarget: boolean;
  /** 1회성 실행을 위해 입력 중인 년도 */
  runYear: string;
  /** 1회성 실행을 위해 입력 중인 학기 코드 */
  runSemester: string;
  /** 1회성 년도 입력 변경 핸들러 */
  onRunYearChange: (value: string) => void;
  /** 1회성 학기 코드 입력 변경 핸들러 */
  onRunSemesterChange: (value: string) => void;
  /** 입력한 특정 값으로 크롤링을 1회 즉시 실행합니다. */
  onRunCustomTarget: () => void;
  /** 1회성 실행 진행 상태 */
  isRunningCustomTarget: boolean;
  /** 1회성 실행 가능 여부 */
  canRunCustomTarget: boolean;
}

/**
 * 관리자 크롤링 타겟 설정/실행 패널 컴포넌트입니다.
 */
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
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl shadow-slate-200/40 lg:rounded-[2.5rem] lg:p-8"
    >
      <div className="mb-6 flex items-center gap-2">
        <Settings2 className="h-5 w-5 text-primary" />
        <h3 className="text-xl font-black tracking-tight text-slate-900">크롤링 대상 제어</h3>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
          <h4 className="mb-3 text-sm font-black text-slate-900">기본 대상 설정</h4>
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
          <h4 className="mb-3 text-sm font-black text-slate-900">특정 대상 즉시 실행</h4>
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

      <div className="mt-4 rounded-xl bg-slate-50 px-3 py-2 text-[11px] font-medium text-slate-500">
        학기 선택: 1학기, 2학기, 하기/동기 계절학기, 여름/겨울/신입생/SW 특별학기
      </div>
    </motion.section>
  );
}
