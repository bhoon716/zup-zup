"use client";

import { cn } from "@/shared/lib/utils";
import type { AdminTrafficPointResponse, AdminTrafficRange } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/shared/ui/tooltip";

const RANGES: ReadonlyArray<{ id: AdminTrafficRange; label: string }> = [
  { id: "LAST_24_HOURS", label: "24시간" },
  { id: "LAST_7_DAYS", label: "7일" },
];

const SERIES = [
  { key: "seatOpened", label: "빈자리", color: "bg-teal-600" },
  { key: "feedbackReply", label: "문의 답변", color: "bg-blue-600" },
  { key: "announcement", label: "중요 공지", color: "bg-amber-500" },
] as const;

const AXIS_INTERVALS = 4;

function getBucketTotal(point: AdminTrafficPointResponse) {
  return SERIES.reduce((total, { key }) => total + point[key], 0);
}

interface AdminTrafficChartProps {
  range: AdminTrafficRange;
  onRangeChange: (range: AdminTrafficRange) => void;
  traffic?: AdminTrafficPointResponse[];
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
}

export function AdminTrafficChart({ range, onRangeChange, traffic, isLoading, isError, onRetry }: AdminTrafficChartProps) {
  const periodTotal = traffic?.reduce((total, point) => total + getBucketTotal(point), 0);

  return (
    <section className="min-w-0 rounded-xl border border-slate-200 bg-white p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight text-slate-900">알림 트래픽 분석</h2>
          <p className="mt-1 text-sm text-slate-600">한국 시간 기준으로 외부 채널 발송이 성공한 건수입니다.</p>
        </div>
        <div role="group" aria-label="알림 발송 기간" className="flex gap-0.5 rounded-lg bg-slate-100 p-0.5">
          {RANGES.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              aria-pressed={range === id}
              onClick={() => onRangeChange(id)}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
                range === id ? "bg-white text-primary shadow-sm" : "text-slate-600 hover:text-primary",
              )}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2 border-t border-slate-100 pt-3">
        <ul aria-label="알림 종류" className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-slate-700">
          {SERIES.map(({ key, label, color }) => (
            <li key={key} className="flex items-center gap-1.5">
              <span aria-hidden="true" className={cn("size-2.5 rounded-sm", color)} />
              {label}
            </li>
          ))}
        </ul>
        {periodTotal !== undefined && (
          <p className="whitespace-nowrap text-sm text-slate-600">
            기간 합계 <strong className="font-semibold tabular-nums text-slate-900">{periodTotal.toLocaleString("ko-KR")}건</strong>
          </p>
        )}
      </div>

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
  const peak = Math.max(...traffic.map(getBucketTotal));
  const rawStep = Math.max(peak / AXIS_INTERVALS, 1);
  const magnitude = 10 ** Math.floor(Math.log10(rawStep));
  const tickStep = Math.ceil(rawStep / magnitude) * magnitude;
  const axisMax = tickStep * AXIS_INTERVALS;
  const ticks = Array.from({ length: AXIS_INTERVALS + 1 }, (_, index) => axisMax - index * tickStep);
  const labelIndices = new Set([0, Math.floor(traffic.length / 2), traffic.length - 1]);

  return (
    <div className="mt-4 grid grid-cols-[auto_minmax(0,1fr)] gap-x-2 sm:gap-x-3">
      <div aria-hidden="true" className="flex h-44 flex-col justify-between text-right text-xs leading-none tabular-nums text-slate-600 sm:h-60">
        {ticks.map((tick) => (
          <span key={tick} className="h-0 -translate-y-1.5">{tick.toLocaleString("ko-KR")}</span>
        ))}
      </div>
      <div className="relative flex items-start">
        <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 top-0 flex h-44 flex-col justify-between sm:h-60">
          {ticks.map((tick) => (
            <div
              key={tick}
              className={tick === 0 ? "h-0 border-t border-slate-300" : "h-0 border-t border-dashed border-slate-200"}
            />
          ))}
        </div>
        {traffic.map((point, index) => (
          <div className="min-w-0 flex-1" key={point.bucketStart}>
            <Tooltip delayDuration={100}>
              <TooltipTrigger asChild>
                <button
                  type="button"
                  aria-label={`${point.label}, ${SERIES.map(({ key, label }) => `${label} ${point[key]}건`).join(", ")}`}
                  className="relative flex h-44 w-full flex-col-reverse items-center rounded-sm focus-visible:z-10 focus-visible:outline-2 focus-visible:outline-primary data-[state=delayed-open]:bg-primary/5 data-[state=instant-open]:bg-primary/5 sm:h-60"
                >
                  {SERIES.map(({ key, color }) => (
                    <span
                      aria-hidden="true"
                      className={cn("w-2/3 max-w-12", color)}
                      key={key}
                      style={{ height: `${point[key] / axisMax * 100}%` }}
                    />
                  ))}
                </button>
              </TooltipTrigger>
              <TooltipContent side="right" sideOffset={6} collisionPadding={12} className="min-w-28 px-3 py-2 text-xs">
                <p className="font-semibold">{point.label}</p>
                <dl className="mt-1 grid grid-cols-[auto_auto] gap-x-3 gap-y-0.5">
                  {SERIES.map(({ key, label, color }) => (
                    <div key={key} className="contents">
                      <dt className="flex items-center gap-1.5">
                        <span aria-hidden="true" className={cn("size-1.5 rounded-full", color)} />
                        {label}
                      </dt>
                      <dd className="text-right tabular-nums">{point[key].toLocaleString("ko-KR")}건</dd>
                    </div>
                  ))}
                </dl>
              </TooltipContent>
            </Tooltip>
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
  );
}
