"use client";

import { useState } from "react";
import {
  AlertCircle,
  BellRing,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCcw,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";

import {
  useAdminDlqNotificationDeliveries,
  useAdminNotificationDelivery,
  useReplayAdminNotificationDelivery,
} from "@/features/admin/hooks/useAdminNotificationDeliveries";
import type {
  AdminNotificationDeliveryResponse,
  NotificationDeliveryChannel,
} from "@/shared/types/api";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";

const CHANNEL_LABELS: Record<NotificationDeliveryChannel, string> = {
  EMAIL: "이메일",
  WEB: "웹 푸시",
  DISCORD: "디스코드",
};

const failureReason = (delivery: AdminNotificationDeliveryResponse) => {
  if (!delivery.lastError) return "-";
  return delivery.lastErrorReason || "기타 수신 실패";
};

const failureSummary = (delivery: AdminNotificationDeliveryResponse) => {
  if (!delivery.lastError) return "-";
  return `${delivery.lastError} · ${failureReason(delivery)}`;
};

const formatDateTime = (value: string | null) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
};

function DeliveryStatusBadge({ delivery }: { delivery: AdminNotificationDeliveryResponse }) {
  return (
    <Badge
      variant="outline"
      className={delivery.status === "DLQ"
        ? "border-red-200 bg-red-50 text-red-600"
        : "border-slate-200 bg-slate-50 text-slate-600"}
    >
      {delivery.status}
    </Badge>
  );
}

export default function AdminNotificationDeliveriesPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const {
    data: dlqPage,
    isLoading: isListLoading,
    isFetching: isListFetching,
    isError: isListError,
    refetch: refetchList,
  } = useAdminDlqNotificationDeliveries(page);
  const {
    data: selectedDelivery,
    isLoading: isDetailLoading,
    isFetching: isDetailFetching,
    isError: isDetailError,
    refetch: refetchDetail,
  } = useAdminNotificationDelivery(selectedId);
  const replayMutation = useReplayAdminNotificationDelivery();
  const totalPages = dlqPage?.totalPages ?? 0;
  const lastPage = Math.max(totalPages - 1, 0);

  if (dlqPage && !isListFetching && !isListError && page > lastPage) {
    setSelectedId(null);
    setPage(lastPage);
  }

  const moveToPage = (nextPage: number) => {
    setSelectedId(null);
    setPage(Math.min(Math.max(nextPage, 0), lastPage));
  };

  const refreshDeliveries = () => {
    void refetchList({ cancelRefetch: false });
    if (selectedId !== null) {
      void refetchDetail({ cancelRefetch: false });
    }
  };

  const handleReplay = async () => {
    if (selectedId === null) return;
    if (!window.confirm("원인 수정이 확인된 선택한 발송 작업만 재처리합니다. 계속할까요?")) return;

    try {
      await replayMutation.mutateAsync({ id: selectedId, request: {} });
      setSelectedId(null);
    } catch {
    }
  };

  return (
    <div className="min-h-[calc(100dvh-4rem)] bg-[#f3f4f6] text-slate-900">
      <main className="mx-auto max-w-7xl space-y-6 px-4 py-6 sm:px-6 lg:px-8">
        <section className="flex items-start gap-3 border-b border-slate-200 pb-5">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-red-50 text-red-500">
            <BellRing className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900">알림 DLQ 관리</h1>
            <p className="mt-1 text-sm text-slate-500">
              실패 원인을 해결한 뒤 해당 알림을 다시 발송할 수 있습니다.
            </p>
          </div>
        </section>

        <section className="grid min-h-[560px] grid-cols-1 gap-6 lg:grid-cols-12">
          <div className="flex min-h-[360px] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm lg:col-span-5">
            <div className="flex items-center justify-between gap-2 border-b border-slate-100 bg-slate-50/70 px-4 py-3">
              <h2 className="text-xs font-black uppercase tracking-wider text-slate-500">
                DLQ 목록 ({dlqPage?.totalElements ?? 0}건)
              </h2>
              <Button
                type="button"
                variant="outline"
                className="h-8 gap-1 rounded-lg border-slate-200 bg-slate-50 px-3 text-xs font-medium text-slate-600 hover:bg-slate-100"
                onClick={refreshDeliveries}
                disabled={isListFetching}
              >
                <RefreshCcw className="h-3.5 w-3.5" />
                새로고침
              </Button>
            </div>

            <div className="flex-1 divide-y divide-slate-100 overflow-y-auto">
              {isListLoading ? (
                <div className="flex h-48 items-center justify-center text-slate-400">
                  <Loader2 className="h-5 w-5 animate-spin" />
                </div>
              ) : isListError ? (
                <div className="p-8 text-center text-sm text-red-500">DLQ 목록을 불러오지 못했습니다.</div>
              ) : dlqPage?.content.length ? (
                dlqPage.content.map((delivery) => (
                  <button
                    key={delivery.id}
                    type="button"
                    onClick={() => setSelectedId(delivery.id)}
                    aria-pressed={selectedId === delivery.id}
                    className={`w-full p-4 text-left transition-colors hover:bg-slate-50 ${
                      selectedId === delivery.id ? "border-l-4 border-primary bg-primary/5" : "border-l-4 border-transparent"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-bold text-slate-800">{delivery.courseName}</p>
                        <p className="mt-1 text-xs font-medium text-slate-400">
                          {delivery.courseKey} · {CHANNEL_LABELS[delivery.channel]} · 시도 {delivery.attempts}회
                        </p>
                      </div>
                      <DeliveryStatusBadge delivery={delivery} />
                    </div>
                    <p className="mt-2 truncate text-[11px] text-red-500">실패 사유: {failureSummary(delivery)}</p>
                  </button>
                ))
              ) : (
                <div className="flex h-48 flex-col items-center justify-center px-6 text-center text-slate-400">
                  <ShieldCheck className="mb-3 h-8 w-8" />
                  <p className="text-sm font-semibold">현재 재처리할 DLQ delivery가 없습니다.</p>
                </div>
              )}
            </div>
            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-slate-100 bg-slate-50/70 px-4 py-3">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  aria-label="이전 페이지"
                  onClick={() => moveToPage(page - 1)}
                  disabled={page === 0 || isListLoading}
                  className="gap-1 text-xs text-slate-600"
                >
                  <ChevronLeft className="h-4 w-4" />
                  이전
                </Button>
                <span className="text-xs font-semibold text-slate-500" aria-live="polite">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  aria-label="다음 페이지"
                  onClick={() => moveToPage(page + 1)}
                  disabled={page + 1 >= totalPages || isListLoading}
                  className="gap-1 text-xs text-slate-600"
                >
                  다음
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </div>

          <div className="flex min-h-[360px] flex-col rounded-2xl border border-slate-200 bg-white shadow-sm lg:col-span-7">
            {selectedId === null ? (
              <div className="flex flex-1 flex-col items-center justify-center p-10 text-center text-slate-400">
                <BellRing className="mb-4 h-12 w-12" />
                <p className="text-base font-bold">확인할 DLQ delivery를 선택해주세요.</p>
              </div>
            ) : isDetailLoading ? (
              <div className="flex flex-1 items-center justify-center text-slate-400">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            ) : isDetailError || !selectedDelivery ? (
              <div className="flex flex-1 flex-col items-center justify-center p-10 text-center text-red-500">
                <AlertCircle className="mb-3 h-8 w-8" />
                <p className="text-sm font-semibold">delivery 상세를 불러오지 못했습니다.</p>
              </div>
            ) : (
              <div className="flex flex-1 flex-col">
                <div className="border-b border-slate-100 p-6">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <div className="mb-2 flex items-center gap-2">
                        <DeliveryStatusBadge delivery={selectedDelivery} />
                        <span className="text-xs font-medium text-slate-400">발송 작업 #{selectedDelivery.id}</span>
                      </div>
                      <h2 className="text-xl font-black text-slate-900">{selectedDelivery.courseName}</h2>
                      <p className="mt-1 text-sm font-medium text-slate-500">{selectedDelivery.courseKey}</p>
                    </div>
                    <Button
                      type="button"
                      onClick={() => void handleReplay()}
                      disabled={isDetailFetching || replayMutation.isPending || selectedDelivery.status !== "DLQ"}
                      className="gap-2 rounded-xl bg-primary text-xs font-bold text-white hover:bg-primary-dark"
                    >
                      {replayMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <RotateCcw className="h-4 w-4" />}
                      선택한 발송 작업 재처리
                    </Button>
                  </div>
                </div>

                <dl className="grid gap-x-6 gap-y-4 p-6 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-xs font-bold text-slate-400">발송 대기열 ID</dt>
                    <dd className="mt-1 font-semibold text-slate-700">{selectedDelivery.outboxId}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold text-slate-400">채널</dt>
                    <dd className="mt-1 font-semibold text-slate-700">{CHANNEL_LABELS[selectedDelivery.channel]}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold text-slate-400">시도 횟수</dt>
                    <dd className="mt-1 font-semibold text-slate-700">{selectedDelivery.attempts}회</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold text-slate-400">마지막 오류 코드</dt>
                    <dd className="mt-1 font-mono text-sm font-semibold text-red-600">{selectedDelivery.lastError || "-"}</dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="text-xs font-bold text-slate-400">마지막 오류 사유</dt>
                    <dd className="mt-1 font-semibold text-red-600">{failureReason(selectedDelivery)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold text-slate-400">DLQ 전환 시각</dt>
                    <dd className="mt-1 font-semibold text-slate-700">{formatDateTime(selectedDelivery.deadLetteredAt)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-bold text-slate-400">중복 제어</dt>
                    <dd className="mt-1 font-semibold text-slate-700">
                      {selectedDelivery.idempotencyKeyRetained ? "idempotency key 유지" : "키 보존 상태 미확인"}
                    </dd>
                  </div>
                </dl>

                <div className="mt-auto border-t border-slate-100 bg-slate-50/70 px-6 py-4 text-xs leading-relaxed text-slate-500">
                  재처리 요청은 관리자 감사 로그에 기록됩니다. 수신자, 원본 idempotency key, provider 예외 원문은 이 화면에 표시하지 않습니다.
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
