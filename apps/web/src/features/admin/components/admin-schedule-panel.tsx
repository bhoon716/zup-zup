import { useState } from "react";
import { motion } from "framer-motion";
import { Calendar, Plus, Trash2, Edit2, Loader2, Save, X } from "lucide-react";
import {
  useAdminSchedules,
  useCreateSchedule,
  useDeleteSchedule,
  useUpdateSchedule,
} from "@/features/admin/hooks/useAdminSchedules";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import type { ScheduleResponse, ScheduleRequest } from "@/shared/types/api";

export function AdminSchedulePanel() {
  const { data: schedules, isLoading } = useAdminSchedules();
  const { mutate: createSchedule, isPending: isCreating } = useCreateSchedule();
  const { mutate: updateSchedule, isPending: isUpdating } = useUpdateSchedule();
  const { mutate: deleteSchedule, isPending: isDeleting } = useDeleteSchedule();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draftTitle, setDraftTitle] = useState("");
  const [draftDate, setDraftDate] = useState("");
  const [draftTime, setDraftTime] = useState("");

  const [isAdding, setIsAdding] = useState(false);

  /**
   * 입력 폼의 상태를 초기화하고 편집/추가 모드를 종료합니다.
   */
  const resetForm = () => {
    setDraftTitle("");
    setDraftDate("");
    setDraftTime("");
    setEditingId(null);
    setIsAdding(false);
  };

  /**
   * 선택된 기존 휴일/일정 정보를 입력 폼에 바인딩하여 편집 모드로 진입합니다.
   */
  const handleEdit = (schedule: ScheduleResponse) => {
    setEditingId(schedule.id);
    setDraftTitle(schedule.title);
    setDraftDate(schedule.scheduleDate);
    setDraftTime(schedule.scheduleTime || "");
    setIsAdding(false);
  };

  /**
   * 편집 또는 생성 중인 일정 데이터를 백엔드로 전송하여 저장합니다.
   */
  const handleSave = () => {
    const request: ScheduleRequest = {
      title: draftTitle,
      scheduleDate: draftDate,
      scheduleTime: draftTime || undefined,
    };

    if (editingId) {
      updateSchedule({ id: editingId, request }, { onSuccess: resetForm });
    } else {
      createSchedule(request, { onSuccess: resetForm });
    }
  };

  const isSaving = isCreating || isUpdating;

  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="col-span-1 rounded-[2.5rem] bg-white p-8 shadow-2xl shadow-slate-200/50 md:p-10 lg:col-span-3"
    >
      <div className="mb-8 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex items-center justify-center h-14 w-14 rounded-2xl bg-indigo-50 text-indigo-500 shadow-inner">
            <Calendar className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900">단일 일정 관리</h2>
            <p className="mt-1 text-sm font-medium text-slate-500">
              대시보드에 표시될 주요 일정을 추가, 수정, 삭제합니다.
            </p>
          </div>
        </div>
        {!isAdding && !editingId && (
          <Button
            onClick={() => setIsAdding(true)}
            className="rounded-xl px-4 py-2 font-medium bg-slate-900 hover:bg-slate-800 text-white"
          >
            <Plus className="mr-2 h-4 w-4" />
            일정 추가
          </Button>
        )}
      </div>

      <div className="space-y-4">
        {/* Form Area */}
        {(isAdding || editingId) && (
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end bg-slate-50 p-6 rounded-2xl border border-slate-100">
            <div className="md:col-span-4 space-y-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">일정 이름</label>
              <Input
                value={draftTitle}
                onChange={(e) => setDraftTitle(e.target.value)}
                placeholder="예: 수강신청 장바구니"
                className="bg-white"
              />
            </div>
            <div className="md:col-span-3 space-y-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">날짜</label>
              <Input
                type="date"
                value={draftDate}
                onChange={(e) => setDraftDate(e.target.value)}
                className="bg-white"
              />
            </div>
            <div className="md:col-span-3 space-y-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">시간 (선택)</label>
              <Input
                type="time"
                step="2"
                value={draftTime}
                onChange={(e) => setDraftTime(e.target.value)}
                className="bg-white"
              />
            </div>
            <div className="md:col-span-2 flex items-center justify-end gap-2 pb-0.5">
              <Button
                variant="outline"
                onClick={resetForm}
                disabled={isSaving}
                className="rounded-xl text-slate-500 hover:text-slate-700"
              >
                <X className="h-4 w-4" />
              </Button>
              <Button
                onClick={handleSave}
                disabled={isSaving || !draftTitle || !draftDate}
                className="rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white"
              >
                {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        )}

        {/* List Area */}
        <div className="mt-6">
          {isLoading ? (
            <div className="flex h-32 items-center justify-center">
              <Loader2 className="h-6 w-6 animate-spin text-slate-300" />
            </div>
          ) : schedules?.length === 0 ? (
            <div className="flex h-32 items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
              <p className="text-sm font-medium text-slate-500">등록된 일정이 없습니다.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {schedules?.map((schedule: ScheduleResponse) => (
                <div
                  key={schedule.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-[0_2px_10px_-4px_rgba(0,0,0,0.05)] transition-all hover:border-slate-200 hover:shadow-md"
                >
                  <div>
                    <div className="flex items-center gap-3">
                      <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-black tracking-widest text-slate-600">
                        {schedule.dDay}
                      </span>
                      <h3 className="font-bold text-slate-900">{schedule.title}</h3>
                    </div>
                    <div className="mt-2 text-sm font-medium text-slate-500">
                      {schedule.scheduleDate} {schedule.scheduleTime || ""}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 self-end sm:self-auto">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEdit(schedule)}
                      disabled={isDeleting || isSaving}
                      className="h-9 w-9 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                    >
                      <Edit2 className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => deleteSchedule(schedule.id)}
                      disabled={isDeleting || isSaving}
                      className="h-9 w-9 text-slate-400 hover:bg-red-50 hover:text-red-600"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </motion.section>
  );
}
