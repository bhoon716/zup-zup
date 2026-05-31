"use client";

import { motion } from "framer-motion";
import { Loader2, Save, Settings2, Shuffle } from "lucide-react";

import { Button } from "@/shared/ui/button";
import { CRAWL_SEMESTER_OPTIONS } from "@/features/admin/lib/crawl-semester-options";

interface AdminSearchDefaultSemesterPanelProps {
  configuredSemester: string;
  onConfiguredSemesterChange: (value: string) => void;
  onSaveConfiguredSemester: () => void;
  onSyncWithCrawlTarget: () => void;
  crawlTargetSemester: string;
  isConfiguredSemesterLoading: boolean;
  isSavingConfiguredSemester: boolean;
  canSaveConfiguredSemester: boolean;
}

export function AdminSearchDefaultSemesterPanel({
  configuredSemester,
  onConfiguredSemesterChange,
  onSaveConfiguredSemester,
  onSyncWithCrawlTarget,
  crawlTargetSemester,
  isConfiguredSemesterLoading,
  isSavingConfiguredSemester,
  canSaveConfiguredSemester,
}: AdminSearchDefaultSemesterPanelProps) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl shadow-slate-200/40 lg:rounded-[2.5rem] lg:p-8"
    >
      <div className="mb-6 flex items-center gap-2">
        <Settings2 className="h-5 w-5 text-primary" />
        <h3 className="text-xl font-black tracking-tight text-slate-900">검색 기본 학기</h3>
      </div>

      <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-5">
        <h4 className="mb-3 text-sm font-black text-slate-900">검색 페이지 초기값</h4>
        <p className="mb-4 text-xs font-medium text-slate-500">
          강의 검색 페이지에서 처음 열릴 때 사용할 학기를 저장합니다. 크롤링 대상과는 별도로 관리됩니다.
        </p>
        <div className="space-y-3">
          <select
            value={configuredSemester}
            onChange={(event) => onConfiguredSemesterChange(event.target.value)}
            disabled={isConfiguredSemesterLoading || isSavingConfiguredSemester}
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
              onClick={onSaveConfiguredSemester}
              disabled={isConfiguredSemesterLoading || isSavingConfiguredSemester || !canSaveConfiguredSemester}
              className="h-9 rounded-xl bg-primary px-4 text-xs font-bold text-white hover:bg-primary-dark"
            >
              {isSavingConfiguredSemester ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Save className="mr-1 h-4 w-4" />}
              저장
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={onSyncWithCrawlTarget}
              disabled={isConfiguredSemesterLoading || isSavingConfiguredSemester || !crawlTargetSemester}
              className="h-9 rounded-xl px-4 text-xs font-bold"
            >
              <Shuffle className="mr-1 h-4 w-4" />
              크롤링 대상과 동일하게 저장
            </Button>
          </div>
        </div>
      </div>

      <div className="mt-4 rounded-xl bg-slate-50 px-3 py-2 text-[11px] font-medium text-slate-500">
        현재 크롤링 대상 학기: {crawlTargetSemester || "미설정"}
      </div>
    </motion.section>
  );
}
