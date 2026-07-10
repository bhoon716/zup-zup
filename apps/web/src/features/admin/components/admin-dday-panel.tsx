import { useState } from "react";
import { motion } from "framer-motion";
import { CalendarRange, Plus, Trash2, Edit2, Loader2, Save, X, Star } from "lucide-react";
import {
  useAdminDdays,
  useActiveDday,
  useCreateDday,
  useUpdateDday,
  useDeleteDday,
} from "@/features/admin/hooks/useDday";
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
import type { DdaySettingResponse, DdaySettingRequest } from "@/shared/types/api";

/**
 * 5분 단위 시간 선택 컴포넌트
 */
function TimePicker({ 
  value, 
  onChange,
  clearLabel = "선택한 시간 초기화",
}: { 
  value: string; 
  onChange: (val: string) => void; 
  clearLabel?: string;
}) {
  const currentHour = value ? value.split(":")[0] : "";
  const currentMinute = value ? value.split(":")[1] : "";

  const hours = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, "0")); // 00시 ~ 23시
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
          aria-label={clearLabel}
          title={clearLabel}
          onClick={() => onChange("")}
        >
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}

const DDAY_PRESETS = [
  "1학기 종강",
  "하기 계절학기 종강",
  "2학기 종강",
  "동기 계절학기 종강",
  "기타 - 직접 입력",
];

/**
 * D-Day가 지났는지 판별
 */
function isPastDday(dday: DdaySettingResponse): boolean {
  return dday.dDay.startsWith("D+");
}

export function AdminDdayPanel() {
  const { data: ddays, isLoading } = useAdminDdays();
  const { data: activeDday } = useActiveDday();
  const { mutate: createDday, isPending: isCreating } = useCreateDday();
  const { mutate: updateDday, isPending: isUpdating } = useUpdateDday();
  const { mutate: deleteDday, isPending: isDeleting } = useDeleteDday();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [draftTitle, setDraftTitle] = useState("");
  const [draftTargetDate, setDraftTargetDate] = useState("");
  const [draftTargetTime, setDraftTargetTime] = useState("");

  const [isAdding, setIsAdding] = useState(false);
  const [isCustomTitle, setIsCustomTitle] = useState(false);

  const resetForm = () => {
    setDraftTitle("");
    setDraftTargetDate("");
    setDraftTargetTime("");
    setEditingId(null);
    setIsAdding(false);
    setIsCustomTitle(false);
  };

  const handleEdit = (dday: DdaySettingResponse) => {
    setEditingId(dday.id);
    setDraftTitle(dday.title);
    
    if (!DDAY_PRESETS.includes(dday.title)) {
      setIsCustomTitle(true);
    } else {
      setIsCustomTitle(false);
    }
    
    setDraftTargetDate(dday.targetDate);
    setDraftTargetTime(dday.targetTime || "");
    setIsAdding(false);
  };

  const handleSave = () => {
    const request: DdaySettingRequest = {
      title: draftTitle,
      targetDate: draftTargetDate,
      targetTime: draftTargetTime || undefined,
    };

    if (editingId) {
      updateDday({ id: editingId, request }, { onSuccess: resetForm });
    } else {
      createDday(request, { onSuccess: resetForm });
    }
  };

  const isSaving = isCreating || isUpdating;

  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="col-span-1 rounded-[2.5rem] bg-white p-8 shadow-2xl shadow-slate-200/50 md:p-10 lg:col-span-3 mt-8"
    >
      <div className="mb-8 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex items-center justify-center h-14 w-14 rounded-2xl bg-primary/10 text-primary shadow-inner">
            <CalendarRange className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900">학사 D-Day 관리</h2>
            <p className="mt-1 text-sm font-medium text-slate-500">
              대시보드와 메인 화면에 띄울 종강 및 학사 D-Day 일정을 관리합니다. (여러 개 등록 시 가장 빠른 일정 자동 선택 노출)
            </p>
          </div>
        </div>
        {!isAdding && !editingId && (
          <Button
            onClick={() => setIsAdding(true)}
            className="rounded-xl px-4 py-2 font-medium bg-slate-900 hover:bg-slate-800 text-white"
          >
            <Plus className="mr-2 h-4 w-4" />
            D-Day 추가
          </Button>
        )}
      </div>

      <div className="space-y-4">
        {/* Form Area */}
        {(isAdding || editingId) && (
          <div className="flex flex-col gap-6 bg-slate-50/50 p-6 rounded-[2rem] border border-slate-100 mb-8">
            <div className="flex flex-col xl:flex-row gap-6 xl:gap-20 xl:items-end">
              {/* D-Day 명칭 */}
              <div className="space-y-2 xl:w-[320px] shrink-0">
                <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1">D-Day 명칭</label>
                {!isCustomTitle ? (
                  <Select
                    value={draftTitle || undefined}
                    onValueChange={(val) => {
                      if (val === "기타 - 직접 입력") {
                        setIsCustomTitle(true);
                        setDraftTitle("");
                      } else {
                        setDraftTitle(val);
                      }
                    }}
                  >
                    <SelectTrigger className="w-full bg-white h-11 rounded-xl border-slate-200">
                      <SelectValue placeholder="D-Day 명칭 선택" />
                    </SelectTrigger>
                    <SelectContent>
                      {DDAY_PRESETS.map((opt) => (
                        <SelectItem key={opt} value={opt}>
                          {opt}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  <div className="flex items-center gap-2 w-full">
                    <Input
                      value={draftTitle}
                      onChange={(e) => setDraftTitle(e.target.value)}
                      placeholder="D-Day 명칭 직접 입력"
                      className="bg-white flex-1 h-11 rounded-xl border-slate-200 min-w-0"
                      autoFocus
                    />
                    <Button
                      variant="outline"
                      size="icon"
                      className="shrink-0 h-11 w-11 rounded-xl border-slate-200"
                      aria-label="D-Day 직접 입력 취소"
                      title="D-Day 직접 입력 취소"
                      onClick={() => {
                        setIsCustomTitle(false);
                        setDraftTitle("");
                      }}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                )}
              </div>

              {/* 목표 일시 */}
              <div className="flex flex-col md:flex-row flex-1 gap-12 md:items-end">
                <div className="flex flex-1 items-center gap-5">
                  <div className="space-y-2 xl:w-[180px]">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1">목표 날짜</label>
                    <Input
                      type="date"
                      value={draftTargetDate}
                      onChange={(e) => setDraftTargetDate(e.target.value)}
                      className="bg-white h-11 rounded-xl border-slate-200"
                    />
                  </div>
                  <div className="space-y-2 shrink-0">
                    <label className="text-[11px] font-black text-slate-400 uppercase tracking-widest ml-1 text-center block">목표 시간 (선택)</label>
                    <TimePicker 
                      value={draftTargetTime} 
                      onChange={setDraftTargetTime}
                      clearLabel="D-Day 시간 초기화"
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
                  disabled={isSaving || !draftTitle || !draftTargetDate}
                  className="h-11 px-6 rounded-xl bg-primary hover:bg-primary/90 text-white shadow-lg shadow-primary/20 font-bold"
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
          ) : ddays?.length === 0 ? (
            <div className="flex h-32 items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
              <p className="text-sm font-medium text-slate-500">등록된 D-Day 일정이 없습니다.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {ddays?.map((dday: DdaySettingResponse) => {
                const isActive = activeDday?.id === dday.id;
                const isPast = isPastDday(dday);

                return (
                  <div
                    key={dday.id}
                    className={`flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border p-5 transition-all ${
                      isActive
                        ? "border-primary/30 bg-primary/10 shadow-[0_2px_12px_-4px_rgba(139,92,246,0.15)] ring-1 ring-primary/30"
                        : isPast
                          ? "border-slate-100/60 bg-slate-50/30 opacity-50"
                          : "border-slate-100 bg-white shadow-[0_2px_10px_-4px_rgba(0,0,0,0.05)] hover:border-slate-200 hover:shadow-md"
                    }`}
                  >
                    <div className="flex flex-col gap-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        {isActive && (
                          <span className="inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs font-black tracking-widest text-primary">
                            <Star className="h-3 w-3 fill-primary text-primary" />
                            현재 표시 중
                          </span>
                        )}
                        <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-black tracking-widest ${
                          isPast
                            ? "bg-slate-100 text-slate-400"
                            : "bg-primary/10 text-primary"
                        }`}>
                          {dday.title}
                        </span>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-black tracking-widest ${
                          isPast
                            ? "bg-slate-100 text-slate-400"
                            : "bg-slate-100 text-slate-600"
                        }`}>
                          {isPast ? "지난 일정" : dday.dDay}
                        </span>
                      </div>
                      <div className={`text-sm font-medium ${isPast ? "text-slate-400" : "text-slate-500"}`}>
                        {dday.targetDate} {dday.targetTime ? dday.targetTime : "하루 종일"}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 self-end sm:self-auto">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleEdit(dday)}
                      disabled={isDeleting || isSaving}
                      className="h-9 w-9 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                      aria-label={`${dday.title} D-Day 수정`}
                      title={`${dday.title} D-Day 수정`}
                    >
                      <Edit2 className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => deleteDday(dday.id)}
                      disabled={isDeleting || isSaving}
                      className="h-9 w-9 text-slate-400 hover:bg-red-50 hover:text-red-600"
                      aria-label={`${dday.title} D-Day 삭제`}
                      title={`${dday.title} D-Day 삭제`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </motion.section>
  );
}
