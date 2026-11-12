"use client";

import { useState } from "react";

import { cn } from "@/shared/lib/utils";
import type { AdminTrafficPointResponse, AdminTrafficRange } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

const RANGES: ReadonlyArray<{ id: AdminTrafficRange; label: string }> = [
  { id: "LAST_24_HOURS", label: "24시간" },
  { id: "LAST_7_DAYS", label: "7일" },
];

const SERIES = [
  { key: "seatOpened", label: "빈자리", color: "bg-teal-600" },
  { key: "feedbackReply", label: "문의 답변", color: "bg-blue-600" },
  { key: "announcement", label: "중요 공지", color: "bg-amber-500" },
] as const;

interface AdminTrafficChartProps {
  range: AdminTrafficRange;
  onRangeChange: (range: AdminTrafficRange) => void;
  traffic?: AdminTrafficPointResponse[];
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
}

export function AdminTrafficChart({ range, onRangeChange, traffic, isLoading, isError, onRetry }: AdminTrafficChartProps) {
  return (
    <section className="min-w-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6 lg:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">알림 트래픽 분석</h2>
          <p className="mt-1 text-sm text-slate-600">한국 시간 기준으로 외부 채널 발송이 성공한 건수입니다.</p>
        </div>
        <div role="group" aria-label="알림 발송 기간" className="flex gap-1 rounded-xl bg-slate-100 p-1">
          {RANGES.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              aria-pressed={range === id}
              onClick={() => onRangeChange(id)}
              className={cn(
                "rounded-lg px-3.5 py-2 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
                range === id ? "bg-white text-primary shadow-sm" : "text-slate-600 hover:text-primary",
              )}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <ul aria-label="알림 종류" className="mt-5 flex flex-wrap gap-x-4 gap-y-2 text-sm text-slate-700">
        {SERIES.map(({ key, label, color }) => (
          <li key={key} className="flex items-center gap-1.5">
            <span className={cn("size-2.5 rounded-sm", color)} />
            {label}
          </li>
        ))}
      </ul>

      {isError && (
        <div className="mt-5 flex flex-wrap items-center gap-3 text-sm text-slate-700" role="status">
          <p>알림 발송량을 불러오지 못했습니다.</p>
          <Button variant="outline" size="sm" onClick={onRetry}>다시 시도</Button>
        </div>
      )}
      {isLoading && <p role="status" className="mt-5 text-sm text-slate-600">알림 발송량을 불러오는 중입니다.</p>}
      {!isLoading && !isError && !traffic?.length && (
        <p className="mt-5 text-sm text-slate-600">표시할 발송 구간이 없습니다.</p>
      )}
      {!!traffic?.length && <TrafficBars key={range} traffic={traffic} />}
    </section>
  );
}

function TrafficBars({ traffic }: { traffic: AdminTrafficPointResponse[] }) {
  const [activeBucket, setActiveBucket] = useState<string>();
  const maxTraffic = Math.max(...traffic.map((point) => SERIES.reduce((sum, { key }) => sum + point[key], 0)), 1);
  const detail = traffic.find((point) => point.bucketStart === activeBucket);
  const labelIndices = new Set([0, Math.floor(traffic.length / 2), traffic.length - 1]);

  return (
    <>
      <div className="relative mt-6">
        <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 top-0 flex h-40 flex-col justify-between sm:h-52">
          {[0, 1, 2, 3, 4].map((line) => (
            <div key={line} className={line === 4 ? "border-t border-slate-300" : "border-t border-dashed border-slate-200"} />
          ))}
        </div>
        <div className="relative flex items-start">
          {traffic.map((point, index) => (
            <div className="min-w-0 flex-1" key={point.bucketStart}>
              <button
                type="button"
                aria-label={`${point.label}, ${SERIES.map(({ key, label }) => `${label} ${point[key]}건`).join(", ")}`}
                onFocus={() => setActiveBucket(point.bucketStart)}
                onPointerEnter={() => setActiveBucket(point.bucketStart)}
                onClick={() => setActiveBucket(point.bucketStart)}
                className={cn(
                  "flex h-40 w-full flex-col-reverse items-center rounded-t-sm focus-visible:z-10 focus-visible:outline-2 focus-visible:outline-primary sm:h-52",
                  detail?.bucketStart === point.bucketStart && "bg-slate-100/70",
                )}
              >
                {SERIES.map(({ key, color }) => (
                  <span
                    aria-hidden="true"
                    className={cn("w-2/3 max-w-12", color)}
                    key={key}
                    style={{ height: `${point[key] / maxTraffic * 100}%` }}
                  />
                ))}
              </button>
              {labelIndices.has(index) && (
                <div aria-hidden="true" className={cn(
                  "mt-2 w-max whitespace-nowrap text-xs tabular-nums text-slate-600",
                  index === 0 ? "" : index === traffic.length - 1 ? "ml-auto float-right" : "relative left-1/2 -translate-x-1/2",
                )}>
                  {point.label}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <p className="mt-4 min-h-10 rounded-xl bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
        {detail ? (
          <>
            <strong className="mr-2 font-semibold">{detail.label}</strong>
            {SERIES.map(({ key, label }) => (
              <span className="mr-3 inline-block" key={key}>{label} {detail[key].toLocaleString("ko-KR")}건</span>
            ))}
          </>
        ) : (
          "막대에 마우스를 올리거나 선택하면 구간별 건수를 볼 수 있습니다."
        )}
      </p>
    </>
  );
}
