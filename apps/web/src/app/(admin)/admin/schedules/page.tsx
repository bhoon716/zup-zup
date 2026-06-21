"use client";

import React, { useState } from "react";
import { 
  useAdminAllSchedules, 
  useCreateSchedule, 
  useUpdateSchedule, 
  useDeleteSchedule 
} from "@/features/schedule/hooks/useSchedules";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Loader2, Plus, Calendar, Trash2, Edit3, X, AlertTriangle } from "lucide-react";
import type { ScheduleResponse, ScheduleRequest } from "@/shared/types/api";

export default function AdminSchedulesPage() {
  const { data: schedules, isLoading } = useAdminAllSchedules();
  
  const createMutation = useCreateSchedule();
  const updateMutation = useUpdateSchedule();
  const deleteMutation = useDeleteSchedule();

  // 모달 및 폼 상태 관리
  const [isOpen, setIsOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<ScheduleResponse | null>(null);

  // 폼 필드 상태
  const [scheduleType, setScheduleType] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");

  const resetForm = () => {
    setScheduleType("");
    setStartDate("");
    setEndDate("");
    setStartTime("");
    setEndTime("");
    setEditingSchedule(null);
  };

  const handleOpenCreate = () => {
    resetForm();
    setIsOpen(true);
  };

  const handleOpenEdit = (schedule: ScheduleResponse) => {
    setEditingSchedule(schedule);
    setScheduleType(schedule.scheduleType);
    setStartDate(schedule.startDate);
    setEndDate(schedule.endDate);
    setStartTime(schedule.startTime || "");
    setEndTime(schedule.endTime || "");
    setIsOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!scheduleType || !startDate || !endDate) {
      alert("일정 종류와 시작/종료일은 필수 항목입니다.");
      return;
    }

    const payload: ScheduleRequest = {
      scheduleType,
      startDate,
      endDate,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
    };

    try {
      if (editingSchedule) {
        await updateMutation.mutateAsync({ id: editingSchedule.id, request: payload });
      } else {
        await createMutation.mutateAsync(payload);
      }
      setIsOpen(false);
      resetForm();
    } catch (err) {
      console.error("일정 처리 중 에러 발생:", err);
      alert("일정 처리 중 오류가 발생했습니다.");
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("정말로 이 일정을 영구 삭제하시겠습니까?")) return;
    try {
      await deleteMutation.mutateAsync(id);
    } catch (err) {
      console.error("일정 삭제 실패:", err);
      alert("일정 삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="space-y-8">
      {/* 어드민 헤더 */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-gray-100 dark:border-gray-800 pb-6">
        <div>
          <h2 className="text-3xl font-black text-gray-900 dark:text-gray-100 tracking-tight">
            학사 및 수강신청 일정 관리
          </h2>
          <p className="text-sm text-muted-foreground mt-1.5">
            메인 대시보드와 비로그인 첫 화면에 렌더링되는 실시간 D-Day와 오아시스 타이머 일정을 동적으로 생성 및 수정합니다.
          </p>
        </div>
        <Button 
          onClick={handleOpenCreate} 
          className="rounded-full bg-primary font-bold text-white px-5 py-4 shrink-0 shadow-lg shadow-primary/20 transition-all flex items-center gap-1.5 hover:bg-primary/95"
        >
          <Plus className="w-4 h-4" />
          새 일정 등록
        </Button>
      </div>

      {/* 일정 경고 안내 뱃지 */}
      <div className="flex gap-3 bg-amber-50/50 dark:bg-amber-950/20 border border-amber-200/50 dark:border-amber-900/30 p-4 rounded-2xl text-xs text-amber-800 dark:text-amber-300 font-medium">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
        <div>
          <p className="font-bold mb-0.5">💡 실시간 종강 시계 동적 연동 가이드</p>
          <p>
            대시보드와 랜딩 화면의 <strong>종강 시계(D-Day 및 실시간 카운트다운)</strong>는 등록된 일정 중 종류에 <strong>&apos;종강&apos;</strong> 키워드가 포함된 일정을 기준으로 자동 동작합니다.<br/>
            (예: 종류에 <code className="bg-amber-100 dark:bg-amber-900/50 px-1 rounded font-bold">1학기 종강일</code> 등을 입력하고 종료 날짜와 시간을 입력하면, 카운트다운 타겟이 실시간 연동됩니다.)
          </p>
        </div>
      </div>

      {/* 테이블 목록 */}
      {isLoading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : schedules?.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-gray-100 dark:border-gray-800 rounded-3xl text-gray-400">
          <Calendar className="w-12 h-12 mx-auto opacity-10 mb-4" />
          <p className="text-sm font-semibold">등록된 일정이 없습니다. 새 일정을 등록해 보세요!</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-900 border border-gray-100 dark:border-gray-800 rounded-3xl overflow-hidden shadow-floating">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50/50 dark:bg-gray-950/30 border-b border-gray-100 dark:border-gray-800 text-[11px] font-black uppercase text-muted-foreground tracking-wider">
                  <th className="p-4 pl-6">종류</th>
                  <th className="p-4">시작 일시</th>
                  <th className="p-4">종료 일시</th>
                  <th className="p-4">D-Day</th>
                  <th className="p-4 pr-6 text-right">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 dark:divide-gray-850 text-sm">
                {schedules?.map((schedule) => {
                  const isEndOfSemester = schedule.scheduleType.includes("종강");
                  return (
                    <tr 
                      key={schedule.id} 
                      className={`hover:bg-gray-50/30 dark:hover:bg-gray-800/10 transition-colors ${
                        isEndOfSemester ? "bg-primary/[0.01] dark:bg-primary/[0.02]" : ""
                      }`}
                    >
                      <td className="p-4 pl-6">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-gray-900 dark:text-gray-100">
                            {schedule.scheduleType}
                          </span>
                          {isEndOfSemester && (
                            <span className="bg-primary/10 text-primary text-[10px] font-black px-2 py-0.5 rounded-full">
                              종강 타겟
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="p-4 text-muted-foreground font-medium">
                        {schedule.startDate} {schedule.startTime ? schedule.startTime : ""}
                      </td>
                      <td className="p-4 text-muted-foreground font-medium">
                        {schedule.endDate} {schedule.endTime ? schedule.endTime : ""}
                      </td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-black ${
                          schedule.dDay === "D-Day" 
                            ? "bg-red-50 text-red-600 dark:bg-red-950/20 dark:text-red-400"
                            : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300"
                        }`}>
                          {schedule.dDay}
                        </span>
                      </td>
                      <td className="p-4 pr-6 text-right space-x-1 shrink-0">
                        <Button
                          variant="ghost"
                          onClick={() => handleOpenEdit(schedule)}
                          className="h-8 w-8 rounded-lg p-0 hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-300"
                        >
                          <Edit3 className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => handleDelete(schedule.id)}
                          className="h-8 w-8 rounded-lg p-0 hover:bg-red-50 dark:hover:bg-red-950/20 text-red-600 dark:text-red-400"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 모달 다이얼로그 (생성 및 수정) */}
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-white dark:bg-gray-900 border border-gray-100 dark:border-gray-800 w-full max-w-md rounded-3xl p-6 shadow-2xl relative flex flex-col gap-6 animate-in zoom-in-95 duration-200">
            <Button
              variant="ghost"
              onClick={() => setIsOpen(false)}
              className="absolute right-4 top-4 h-8 w-8 rounded-full p-0 hover:bg-gray-100 dark:hover:bg-gray-800"
            >
              <X className="w-4 h-4" />
            </Button>

            <div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                {editingSchedule ? "일정 정보 수정" : "새 학사 일정 등록"}
              </h3>
              <p className="text-xs text-muted-foreground mt-1">
                대시보드와 첫 화면에 노출될 수강신청/종강 일정을 입력해 주세요.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <Label className="text-xs font-bold text-gray-700 dark:text-gray-300">일정 종류 (필수)</Label>
                <input
                  type="text"
                  placeholder="예: 1학기 종강일, 수강신청 장바구니"
                  value={scheduleType}
                  onChange={(e) => setScheduleType(e.target.value)}
                  className="w-full h-10 border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-950/20 rounded-xl px-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-gray-700 dark:text-gray-300">시작 날짜 (필수)</Label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full h-10 border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-950/20 rounded-xl px-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-gray-700 dark:text-gray-300">종료 날짜 (필수)</Label>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="w-full h-10 border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-950/20 rounded-xl px-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-gray-700 dark:text-gray-300">시작 시각 (선택)</Label>
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="w-full h-10 border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-950/20 rounded-xl px-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-bold text-gray-700 dark:text-gray-300">종료 시각 (선택)</Label>
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="w-full h-10 border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-950/20 rounded-xl px-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-50 dark:border-gray-850">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsOpen(false)}
                  className="rounded-full px-5 py-3 text-sm font-semibold border-gray-100 hover:bg-gray-50"
                >
                  취소
                </Button>
                <Button
                  type="submit"
                  disabled={createMutation.isPending || updateMutation.isPending}
                  className="rounded-full bg-primary text-white font-bold px-5 py-3 text-sm shadow-md shadow-primary/10 hover:bg-primary/95"
                >
                  {(createMutation.isPending || updateMutation.isPending) && (
                    <Loader2 className="w-4 h-4 animate-spin mr-1.5 inline" />
                  )}
                  {editingSchedule ? "수정 완료" : "등록 완료"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
