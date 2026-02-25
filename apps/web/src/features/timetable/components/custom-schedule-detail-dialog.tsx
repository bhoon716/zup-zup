"use client";

import React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/shared/ui/dialog';
import { Button } from '@/shared/ui/button';
import { MapPin, User, Trash2, Calendar } from 'lucide-react';
import { RenderingBlock, mergeAdjacentSchedules } from '@/features/timetable/lib/timetable';
import { useRemoveCustomSchedule, useTimetableDetail } from '@/features/timetable/hooks/useTimetable';
import { formatDayOfWeek } from '@/shared/lib/formatters';

interface CustomScheduleDetailDialogProps {
  block: RenderingBlock | null;
  timetableId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * 커스텀 일정을 클릭했을 때 보여주는 상세 정보 및 삭제 다용로그입니다.
 */
export function CustomScheduleDetailDialog({ block, timetableId, open, onOpenChange }: CustomScheduleDetailDialogProps) {
  const removeCustomSchedule = useRemoveCustomSchedule();
  const { data: timetable } = useTimetableDetail(timetableId);

  // 현재 클릭된 일정의 전체 정보를 가져옵니다 (같은 그룹의 다른 시간대 포함)
  const fullSchedule = timetable?.customSchedules.find(s => s.id === block?.id);

  if (!block || !fullSchedule) return null;

  const handleDelete = async () => {
    if (!timetableId) return;
    if (confirm('이 일정을 삭제하시겠습니까? (연결된 모든 시간대가 함께 삭제됩니다)')) {
      await removeCustomSchedule.mutateAsync({
        timetableId,
        scheduleId: Number(block.id),
      });
      onOpenChange(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md p-0 overflow-hidden border-none bg-white dark:bg-[#251e2b] rounded-3xl shadow-2xl">
        <div className="px-8 py-8 space-y-6">
          <DialogHeader className="space-y-2 text-left">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-bold uppercase tracking-wider w-fit">
              직접 추가 일정
            </div>
            <DialogTitle className="text-2xl font-black text-gray-900 dark:text-white tracking-tight break-all">
              {fullSchedule.title}
            </DialogTitle>
            <DialogDescription className="sr-only">
              커스텀 일정의 상세 정보입니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {fullSchedule.professor && (
              <div className="flex items-center gap-3 text-gray-600 dark:text-gray-300">
                <div className="w-8 h-8 rounded-lg bg-gray-50 dark:bg-white/5 flex items-center justify-center">
                  <User className="w-4 h-4 text-primary" />
                </div>
                <span className="text-sm font-medium">{fullSchedule.professor}</span>
              </div>
            )}

            <div className="space-y-3">
              <div className="flex items-start gap-3 text-gray-600 dark:text-gray-300">
                <div className="w-8 h-8 rounded-lg bg-gray-50 dark:bg-white/5 flex items-center justify-center shrink-0">
                  <Calendar className="w-4 h-4 text-primary" />
                </div>
                <div className="flex-1 space-y-2">
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-wide block">일정 시간</span>
                  <div className="space-y-1.5">
                    {mergeAdjacentSchedules(fullSchedule.schedules).map((slot) => (
                      <div key={slot.id} className="flex items-center justify-between bg-gray-50 dark:bg-white/5 px-3 py-2 rounded-xl border border-gray-100 dark:border-gray-800">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-primary">{formatDayOfWeek(slot.dayOfWeek)}</span>
                          <span className="text-sm font-mono font-medium text-gray-700 dark:text-gray-200">
                            {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                          </span>
                        </div>
                        {slot.classroom && (
                          <div className="flex items-center gap-1 text-[10px] text-gray-400">
                            <MapPin className="w-3 h-3" />
                            {slot.classroom}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter className="px-8 py-5 bg-gray-50/50 dark:bg-black/10 border-t border-gray-100 dark:border-gray-800 flex flex-row items-center justify-between gap-4">
          <Button
            variant="ghost"
            onClick={() => onOpenChange(false)}
            className="text-gray-500 hover:text-gray-800 dark:hover:text-gray-200"
          >
            닫기
          </Button>
          <Button
            variant="ghost"
            onClick={handleDelete}
            disabled={removeCustomSchedule.isPending}
            className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/10 font-bold flex items-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            일정 삭제
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
