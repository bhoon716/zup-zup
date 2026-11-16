import { useEffect, useId, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { Calendar, Plus, Trash2, Edit2, Loader2, Save, X, RotateCcw } from "lucide-react";
import Link from "next/link";

import {
  useAdminSchedules,
  useCreateSchedule,
  useDeleteSchedule,
  useUpdateSchedule,
  useRestoreSchedule,
} from "@/features/schedule/hooks/useSchedules";
import { localizedErrorMessage } from "@/shared/lib/error-message";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import type { ScheduleResponse, ScheduleRequest } from "@/shared/types/api";

const SCHEDULE_TYPE_OPTIONS = [
  "장바구니(예비)",
  "학부 2~4학년 전공",
  "학부 2~4학년 일반선택",
  "학부 1학년",
  "학부 전체",
  "일반대학원, 특수대학원",
  "수강 정정 기간",
  "수강 취소 기간",
  "종강",
  "기타",
];

type ScheduleEditor = { kind: "create" } | { kind: "edit"; id: ScheduleResponse["id"] };

export function AdminSchedulePanel({ upcomingOnly = false }: { upcomingOnly?: boolean }) {
  const { data: schedules, isLoading } = useAdminSchedules();
  const { mutate: createSchedule, isPending: isCreating } = useCreateSchedule();
  const { mutate: updateSchedule, isPending: isUpdating } = useUpdateSchedule();
  const { mutate: deleteSchedule, isPending: isDeleting } = useDeleteSchedule();
  const { mutate: restoreSchedule, isPending: isRestoring } = useRestoreSchedule();

  const [editor, setEditor] = useState<ScheduleEditor | null>(null);
  const [saveError, setSaveError] = useState("");
  const [actionError, setActionError] = useState("");
  const addButton = useRef<HTMLButtonElement>(null);
  const editorTrigger = useRef<HTMLButtonElement | null>(null);
  const editorId = useId();

  const openEditor = (next: ScheduleEditor, trigger: HTMLButtonElement) => {
    editorTrigger.current = trigger;
    setSaveError("");
    setEditor(next);
  };

  const closeEditor = () => {
    setEditor(null);
    setSaveError("");
    const trigger = editorTrigger.current;
    requestAnimationFrame(() => {
      const target = trigger?.isConnected && !trigger.disabled ? trigger : addButton.current;
      target?.focus();
    });
  };

  const handleSave = (request: ScheduleRequest) => {
    if (!editor) return;

    setSaveError("");
    const options = {
      onSuccess: closeEditor,
      onError: (error: unknown) => setSaveError(scheduleSaveError(error)),
    };
    if (editor.kind === "edit") {
      updateSchedule({ id: editor.id, request }, options);
    } else {
      createSchedule(request, options);
    }
  };

  const handleDelete = (id: number) => {
    if (!window.confirm("이 일정을 삭제하시겠습니까? 관리자 목록에서 다시 복원할 수 있습니다.")) return;
    setActionError("");
    deleteSchedule(id, {
      onSuccess: () => {
        if (editor?.kind === "edit" && editor.id === id) closeEditor();
      },
      onError: () => setActionError("일정을 삭제하지 못했습니다. 다시 시도해 주세요."),
    });
  };

  const handleRestore = (id: number) => {
    setActionError("");
    restoreSchedule(id, {
      onError: () => setActionError("일정을 복원하지 못했습니다. 다시 시도해 주세요."),
    });
  };

  const isSaving = isCreating || isUpdating;
  const isMutating = isSaving || isDeleting || isRestoring;
  const visibleSchedules = upcomingOnly ? filterActiveUpcomingSchedules(schedules) : schedules;
  const fontClassName = upcomingOnly ? "font-schedule" : undefined;
  const editorProps = { id: editorId, isSaving, isMutating, error: saveError, onSave: handleSave, onCancel: closeEditor, fontClassName };

  return (
    <section
      aria-labelledby="admin-schedule-heading"
      className={cn("rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6", fontClassName)}
    >
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id="admin-schedule-heading" className="flex items-center gap-2 text-lg font-bold text-slate-900">
            <Calendar className="size-5 text-primary" />
            수강 신청 일정 관리
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {upcomingOnly ? "진행 중이거나 예정된 일정입니다. 지난 일정과 삭제된 일정은 전체 일정에서 확인할 수 있습니다." : "삭제된 일정을 복원할 수 있습니다."}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {upcomingOnly && (
            <Button asChild variant="outline">
              <Link href="/admin/schedules">전체 일정 관리</Link>
            </Button>
          )}
          <Button
            ref={addButton}
            type="button"
            onClick={(event) => openEditor({ kind: "create" }, event.currentTarget)}
            disabled={isMutating}
            aria-expanded={editor?.kind === "create"}
            aria-controls={editor?.kind === "create" ? editorId : undefined}
          >
            <Plus className="size-4" />
            일정 추가
          </Button>
        </div>
      </div>

      {actionError && <p role="alert" className="mb-3 text-sm text-red-700">{actionError}</p>}
      {editor?.kind === "create" && (
        <div className="mb-4">
          <ScheduleEditorForm {...editorProps} />
        </div>
      )}

      {isLoading ? (
        <div className="flex h-24 items-center justify-center">
          <Loader2 className="size-6 animate-spin text-slate-400" aria-label="일정 불러오는 중" />
        </div>
      ) : visibleSchedules?.length === 0 ? (
        <p className="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-5 text-center text-sm text-slate-500">
          {upcomingOnly ? "진행 중이거나 예정된 일정이 없습니다." : "등록된 일정이 없습니다."}
        </p>
      ) : (
        <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200">
          {visibleSchedules?.map((schedule) => {
            const isEditing = !schedule.deletedAt && editor?.kind === "edit" && editor.id === schedule.id;
            return (
              <li key={schedule.id} className="p-3 sm:px-4">
                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span className="break-words text-sm font-semibold text-slate-900">{schedule.scheduleType}</span>
                      {schedule.ddayEnabled && <span className="rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">대표 D-day</span>}
                      {schedule.deletedAt && <span className="rounded-md bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700">삭제됨</span>}
                      <span className="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">{schedule.dDay}</span>
                    </div>
                    <p className="mt-1 flex flex-wrap gap-x-1 text-sm tabular-nums text-slate-600">
                      <span>{schedule.startDate} {schedule.startTime} ~</span>
                      <span>{schedule.endDate} {schedule.endTime}</span>
                    </p>
                    {schedule.deletedAt && schedule.ddayEnabled && (
                      <p className="mt-1 text-xs text-slate-500">복원하면 이전 대표 D-day 지정도 함께 복원됩니다.</p>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    {schedule.deletedAt ? (
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => handleRestore(schedule.id)}
                        disabled={isMutating}
                        aria-label={`${schedule.scheduleType} 복원`}
                      >
                        <RotateCcw className="size-4" /> 복원
                      </Button>
                    ) : (
                      <>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={(event) => openEditor({ kind: "edit", id: schedule.id }, event.currentTarget)}
                          disabled={isMutating}
                          className="text-slate-600 aria-expanded:bg-primary/10 aria-expanded:text-primary"
                          aria-label={`${schedule.scheduleType} 수정`}
                          title={`${schedule.scheduleType} 수정`}
                          aria-expanded={isEditing}
                          aria-controls={isEditing ? editorId : undefined}
                        >
                          <Edit2 className="size-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDelete(schedule.id)}
                          disabled={isMutating}
                          className="text-slate-600 hover:bg-red-50 hover:text-red-700"
                          aria-label={`${schedule.scheduleType} 삭제`}
                          title={`${schedule.scheduleType} 삭제`}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </>
                    )}
                  </div>
                </div>
                {isEditing && (
                  <div className="mt-3">
                    <ScheduleEditorForm {...editorProps} schedule={schedule} />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function ScheduleEditorForm({ id, schedule, isSaving, isMutating, error, onSave, onCancel, fontClassName }: {
  id: string;
  schedule?: ScheduleResponse;
  isSaving: boolean;
  isMutating: boolean;
  error: string;
  onSave: (request: ScheduleRequest) => void;
  onCancel: () => void;
  fontClassName?: string;
}) {
  const [draft, setDraft] = useState<Required<ScheduleRequest>>(() => ({
    scheduleType: schedule?.scheduleType ?? "",
    startDate: schedule?.startDate ?? "",
    endDate: schedule?.endDate ?? "",
    startTime: schedule?.startTime ?? "",
    endTime: schedule?.endTime ?? "",
    ddayEnabled: schedule?.ddayEnabled ?? false,
  }));
  const [isCustomType, setIsCustomType] = useState(() => !!schedule && !SCHEDULE_TYPE_OPTIONS.includes(schedule.scheduleType));
  const typeSelect = useRef<HTMLButtonElement>(null);
  const typeInput = useRef<HTMLInputElement>(null);
  const typeId = `${id}-type`;
  const errorId = `${id}-error`;

  useEffect(() => {
    const field = isCustomType ? typeInput.current : typeSelect.current;
    field?.focus();
  }, [isCustomType]);

  return (
    <form
      id={id}
      aria-label={schedule ? `${schedule.scheduleType} 수정` : "새 일정 추가"}
      aria-describedby={error ? errorId : undefined}
      className="rounded-xl border border-indigo-100 bg-slate-50 p-3 sm:p-4"
      onSubmit={(event) => {
        event.preventDefault();
        onSave({ ...draft, startTime: draft.startTime || undefined, endTime: draft.endTime || undefined });
      }}
    >
      <fieldset disabled={isMutating} className="min-w-0 space-y-3">
        <div className="grid gap-3 lg:grid-cols-[minmax(9rem,1fr)_minmax(0,4fr)]">
          <div className="min-w-0 space-y-1">
            <label htmlFor={typeId} className="block text-sm font-medium text-slate-700">일정 구분</label>
            {isCustomType ? (
              <div className="flex gap-1">
                <Input
                  id={typeId}
                  ref={typeInput}
                  value={draft.scheduleType}
                  onChange={(event) => setDraft({ ...draft, scheduleType: event.target.value })}
                  placeholder="입력"
                  maxLength={255}
                  required
                  className="bg-white"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-label="수강 일정 직접 입력 취소"
                  onClick={() => {
                    setIsCustomType(false);
                    setDraft({ ...draft, scheduleType: "" });
                  }}
                >
                  <X className="size-4" />
                </Button>
              </div>
            ) : (
              <Select
                value={draft.scheduleType}
                onValueChange={(value) => {
                  setIsCustomType(value === "기타");
                  setDraft({ ...draft, scheduleType: value === "기타" ? "" : value });
                }}
              >
                <SelectTrigger ref={typeSelect} id={typeId} className="w-full bg-white">
                  <SelectValue placeholder="구분 선택" />
                </SelectTrigger>
                <SelectContent className={fontClassName}>
                  {SCHEDULE_TYPE_OPTIONS.map((option) => <SelectItem key={option} value={option}>{option}</SelectItem>)}
                </SelectContent>
              </Select>
            )}
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <ScheduleDateTimeFields
              label="시작"
              id={`${id}-start`}
              date={draft.startDate}
              time={draft.startTime}
              onDateChange={(startDate) => setDraft({ ...draft, startDate })}
              onTimeChange={(startTime) => setDraft({ ...draft, startTime })}
            />
            <ScheduleDateTimeFields
              label="종료"
              id={`${id}-end`}
              date={draft.endDate}
              time={draft.endTime}
              onDateChange={(endDate) => setDraft({ ...draft, endDate })}
              onTimeChange={(endTime) => setDraft({ ...draft, endTime })}
            />
          </div>
        </div>

        <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
          <input
            type="checkbox"
            checked={draft.ddayEnabled}
            onChange={(event) => setDraft({ ...draft, ddayEnabled: event.target.checked })}
            aria-describedby={`${id}-dday`}
            className="size-4 accent-indigo-600"
          />
          대표 D-day로 표시
        </label>
        <p id={`${id}-dday`} className="text-xs text-slate-600">
          여러 일정을 지정하면 진행 중인 일정, 시작이 가까운 일정 순서로 표시합니다.
        </p>
      </fieldset>
      {error && <p id={errorId} role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
      <div className="mt-3 flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isMutating}>
          취소
        </Button>
        <Button type="submit" disabled={isMutating || !draft.scheduleType.trim() || !draft.startDate || !draft.endDate}>
          {isSaving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
          저장
        </Button>
      </div>
    </form>
  );
}

function ScheduleDateTimeFields({ id, label, date, time, onDateChange, onTimeChange }: {
  id: string;
  label: string;
  date: string;
  time: string;
  onDateChange: (value: string) => void;
  onTimeChange: (value: string) => void;
}) {
  return (
    <div className="grid gap-2 min-[380px]:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <div className="min-w-0 space-y-1">
        <label htmlFor={`${id}-date`} className="block text-sm font-medium text-slate-700">{label}일</label>
        <Input id={`${id}-date`} type="date" value={date} required className="bg-white px-2 text-sm"
          onChange={(event) => onDateChange(event.target.value)} />
      </div>
      <div className="min-w-0 space-y-1">
        <label htmlFor={`${id}-time`} className="block text-sm font-medium text-slate-700">{label} 시간</label>
        <div className="flex items-center gap-1">
          <Input id={`${id}-time`} type="time" step={60} value={time} className="bg-white px-2 text-sm"
            onChange={(event) => onTimeChange(event.target.value)} />
          {time && (
            <Button type="button" variant="ghost" size="icon-xs" aria-label={`${label} 시간 초기화`}
              className="text-slate-500 hover:text-red-700" onClick={() => onTimeChange("")}>
              <X className="size-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function filterActiveUpcomingSchedules(schedules: ScheduleResponse[] | undefined) {
  const now = Date.now();
  return schedules?.filter(({ deletedAt, endDate, endTime }) => {
    if (deletedAt) return false;
    const endsAt = Date.parse(`${endDate}T${endTime ?? "24:00"}+09:00`);
    return now < endsAt;
  });
}

function scheduleSaveError(error: unknown) {
  const fallback = "일정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  if (!isAxiosError<unknown>(error)) return fallback;

  const response = error.response;
  if (!response || response.status < 400 || response.status >= 500) return fallback;

  const { data } = response;
  if (typeof data !== "object" || !data || !("message" in data)) return fallback;
  return localizedErrorMessage(data.message, fallback);
}
