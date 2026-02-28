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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  SelectGroup,
  SelectLabel,
} from "@/shared/ui/select";
import type { ScheduleResponse, ScheduleRequest } from "@/shared/types/api";

/**
 * 5분 단위 시간 선택 컴포넌트
 */
function TimePicker({ 
  value, 
  onChange
}: { 
  value: string; 
  onChange: (val: string) => void; 
}) {
  const currentHour = value ? value.split(":")[0] : "";
  const currentMinute = value ? value.split(":")[1] : "";

  const hours = Array.from({ length: 11 }, (_, i) => (i + 8).toString().padStart(2, "0")); // 08시 ~ 18시
  const minutes = Array.from({ length: 12 }, (_, i) => (i * 5).toString().padStart(2, "0"));

  return (
    <div className="flex items-center gap-2">
      <Select 
        value={currentHour} 
        onValueChange={(h) => onChange(`${h}:${currentMinute || "00"}`)}
      >
        <SelectTrigger className="w-[82px] bg-white h-11 rounded-full border-slate-200 px-3">
          <SelectValue placeholder="시" />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            <SelectLabel>시</SelectLabel>
            {hours.map((h) => (
              <SelectItem key={h} value={h}>{h}시</SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <span className="text-slate-300 font-bold">:</span>
      <Select 
        value={currentMinute} 
        onValueChange={(m) => onChange(`${currentHour || "00"}:${m}`)}
      >
        <SelectTrigger className="w-[82px] bg-white h-11 rounded-full border-slate-200 px-3">
          <SelectValue placeholder="분" />
        </SelectTrigger>
        <SelectContent>
          <SelectGroup>
            <SelectLabel>분 (5분)</SelectLabel>
            {minutes.map((m) => (
              <SelectItem key={m} value={m}>{m}분</SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      {value && (
        <Button 
          variant="ghost" 
          size="icon" 
          className="h-11 w-8 text-slate-300 hover:text-red-500 transition-colors"
          onClick={() => onChange("")}
        >
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}

const SCHEDULE_TYPE_OPTIONS = [
  "장바구니(예비)",
  "학부 2~4학년 전공",
  "학부 2~4학년 일반선택",
  "학부 1학년",
  "학부 전체",
  "일반대학원, 특수대학원",
  "수강 정정 기간",
  "수강 취소 기간",
  "기타",
];

export function AdminSchedulePanel() {
  const { data: schedules, isLoading } = useAdminSchedules();
  const { mutate: createSchedule, isPending: isCreating } = useCreateSchedule();
  const { mutate: updateSchedule, isPending: isUpdating } = useUpdateSchedule();
  const { mutate: deleteSchedule, isPending: isDeleting } = useDeleteSchedule();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draftType, setDraftType] = useState("");
  const [draftStartDate, setDraftStartDate] = useState("");
  const [draftEndDate, setDraftEndDate] = useState("");
  const [draftStartTime, setDraftStartTime] = useState("");
  const [draftEndTime, setDraftEndTime] = useState("");

  const [isAdding, setIsAdding] = useState(false);
  const [isCustomType, setIsCustomType] = useState(false);

  /**
   * 입력 폼의 상태를 초기화하고 편집/추가 모드를 종료합니다.
   */
  const resetForm = () => {
    setDraftType("");
    setDraftStartDate("");
    setDraftEndDate("");
    setDraftStartTime("");
    setDraftEndTime("");
    setEditingId(null);
    setIsAdding(false);
    setIsCustomType(false);
  };

  /**
   * 선택된 기존 일정을 입력 폼에 바인딩하여 편집 모드로 진입합니다.
   */
  const handleEdit = (schedule: ScheduleResponse) => {
    setEditingId(schedule.id);
    setDraftType(schedule.scheduleType);
    
    // 기본 프리셋에 없는 경우, 커스텀 타입(기타)으로 간주
    if (!SCHEDULE_TYPE_OPTIONS.includes(schedule.scheduleType)) {
      setIsCustomType(false);
    }
    
    setDraftStartDate(schedule.startDate);
    setDraftEndDate(schedule.endDate);
    setDraftStartTime(schedule.startTime || "");
    setDraftEndTime(schedule.endTime || "");
    setIsAdding(false);
  };

  /**
   * 편집 또는 생성 중인 일정 데이터를 백엔드로 전송하여 저장합니다.
   */
  const handleSave = () => {
    const request: ScheduleRequest = {
      scheduleType: draftType,
      startDate: draftStartDate,
      endDate: draftEndDate,
      startTime: draftStartTime || undefined,
      endTime: draftEndTime || undefined,
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
            <h2 className="text-xl font-bold tracking-tight text-slate-900">수강 신청 일정 관리</h2>
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
          <div className="flex flex-col gap-6 bg-slate-50/50 p-6 rounded-[2rem] border border-slate-100 mb-8">
            <div className="flex flex-col xl:flex-row gap-6 xl:gap-20 xl:items-end">
              {/* 구분 */}
              <div className="space-y-2 xl:w-[320px] shrink-0">
                <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1">구분</label>
                {!isCustomType ? (
                  <Select
                    value={draftType || undefined}
                    onValueChange={(val) => {
                      if (val === "기타") {
                        setIsCustomType(true);
                        setDraftType("");
                      } else {
                        setDraftType(val);
                      }
                    }}
                  >
                    <SelectTrigger className="w-full bg-white h-11 rounded-xl border-slate-200">
                      <SelectValue placeholder="구분 선택" />
                    </SelectTrigger>
                    <SelectContent>
                      {SCHEDULE_TYPE_OPTIONS.map((opt) => (
                        <SelectItem key={opt} value={opt}>
                          {opt}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  <div className="flex items-center gap-2 w-full">
                    <Input
                      value={draftType}
                      onChange={(e) => setDraftType(e.target.value)}
                      placeholder="입력"
                      className="bg-white flex-1 h-11 rounded-xl border-slate-200 min-w-0"
                      autoFocus
                    />
                    <Button
                      variant="outline"
                      size="icon"
                      className="shrink-0 h-11 w-11 rounded-xl border-slate-200"
                      onClick={() => {
                        setIsCustomType(false);
                        setDraftType("");
                      }}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                )}
              </div>

              {/* 시작 및 종료 일시 */}
              <div className="flex flex-col md:flex-row flex-1 gap-12 md:items-end">
                <div className="flex flex-1 items-center gap-5">
                  <div className="space-y-2 xl:w-[140px]">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1">시작일</label>
                    <Input
                      type="date"
                      value={draftStartDate}
                      onChange={(e) => setDraftStartDate(e.target.value)}
                      className="bg-white h-11 rounded-xl border-slate-200"
                    />
                  </div>
                  <div className="space-y-2 shrink-0">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1 text-center block">시작 시간</label>
                    <TimePicker 
                      value={draftStartTime} 
                      onChange={setDraftStartTime} 
                    />
                  </div>
                </div>

                <div className="flex flex-1 items-center gap-5">
                  <div className="space-y-2 xl:w-[140px]">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1">종료일</label>
                    <Input
                      type="date"
                      value={draftEndDate}
                      onChange={(e) => setDraftEndDate(e.target.value)}
                      className="bg-white h-11 rounded-xl border-slate-200"
                    />
                  </div>
                  <div className="space-y-2 shrink-0">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1 text-center block">종료 시간</label>
                    <TimePicker 
                      value={draftEndTime} 
                      onChange={setDraftEndTime} 
                    />
                  </div>
                </div>
              </div>

              {/* 액션 버튼 */}
              <div className="flex items-center gap-1 shrink-0">
                <Button
                  variant="outline"
                  onClick={resetForm}
                  disabled={isSaving}
                  className="h-11 px-4 rounded-xl text-slate-400 hover:text-slate-600 border-slate-200"
                >
                  <X className="h-4 w-4" />
                  취소
                </Button>
                <Button
                  onClick={handleSave}
                  disabled={isSaving || !draftType || !draftStartDate || !draftEndDate}
                  className="h-11 px-6 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-100 font-bold"
                >
                  {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                  저장
                </Button>
              </div>
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
                  <div className="flex flex-col gap-1.5">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="inline-flex items-center rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-black tracking-widest text-indigo-600">
                        {schedule.scheduleType}
                      </span>
                      <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-black tracking-widest text-slate-600">
                        {schedule.dDay}
                      </span>
                    </div>
                    <div className="text-sm font-medium text-slate-500">
                      {schedule.startDate} {schedule.startTime ? schedule.startTime : ""} 
                      {" "}~{" "} 
                      {schedule.endDate} {schedule.endTime ? schedule.endTime : ""}
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
