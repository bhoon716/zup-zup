"use client";

import type { RefObject } from 'react';
import type { CustomScheduleResponse } from '@/shared/types/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/shared/ui/dialog';
import { Button } from '@/shared/ui/button';
import { MapPin, User, Trash2, Calendar } from 'lucide-react';
import { mergeAdjacentSchedules } from '@/features/timetable/lib/timetable';
import { useRemoveCustomSchedule } from '@/features/timetable/hooks/useTimetable';
import { formatDayOfWeek } from '@/shared/lib/formatters';

interface CustomScheduleDetailDialogProps {
  schedule?: CustomScheduleResponse;
  timetableId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  returnFocusRef: RefObject<HTMLButtonElement | null>;
}

export function CustomScheduleDetailDialog({ schedule, timetableId, open, onOpenChange, returnFocusRef }: CustomScheduleDetailDialogProps) {
  const removeCustomSchedule = useRemoveCustomSchedule();

  if (!schedule) return null;

  const handleDelete = async () => {
    if (confirm('이 일정을 삭제하시겠습니까? (연결된 모든 시간대가 함께 삭제됩니다)')) {
      await removeCustomSchedule.mutateAsync({
        timetableId,
        scheduleId: schedule.id,
      });
      onOpenChange(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="max-h-[90dvh] max-w-md overflow-y-auto rounded-2xl border-none bg-card p-0 shadow-xl"
        onCloseAutoFocus={(event) => {
          const trigger = returnFocusRef.current;
          if (!trigger?.isConnected) return;
          event.preventDefault();
          trigger.focus();
        }}
      >
        <div className="space-y-5 px-5 py-6 sm:px-7">
          <DialogHeader className="space-y-1 pr-7 text-left">
            <div className="text-xs font-medium text-muted-foreground">
              직접 추가 일정
            </div>
            <DialogTitle className="text-xl leading-snug font-semibold wrap-anywhere text-foreground">
              {schedule.title}
            </DialogTitle>
            <DialogDescription className="sr-only">
              커스텀 일정의 상세 정보입니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {schedule.professor && (
              <div className="flex items-center gap-3 text-gray-600 dark:text-gray-300">
                <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
                  <User className="w-4 h-4 text-primary" />
                </div>
                <span className="min-w-0 text-sm font-medium wrap-anywhere">{schedule.professor}</span>
              </div>
            )}

            <div className="space-y-3">
              <div className="flex items-start gap-3 text-gray-600 dark:text-gray-300">
                <div className="w-8 h-8 rounded-lg bg-gray-50 dark:bg-white/5 flex items-center justify-center shrink-0">
                  <Calendar className="w-4 h-4 text-primary" />
                </div>
                <div className="min-w-0 flex-1 space-y-2">
                  <span className="block text-xs font-medium text-muted-foreground">일정 시간</span>
                  <div className="space-y-1.5">
                    {mergeAdjacentSchedules(schedule.schedules).map((slot) => (
                      <div key={slot.id} className="flex flex-wrap items-center justify-between gap-x-2 gap-y-1 rounded-lg border border-border bg-muted px-2.5 py-2">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-medium text-primary">{formatDayOfWeek(slot.dayOfWeek)}</span>
                          <span className="text-sm font-medium whitespace-nowrap tabular-nums text-foreground">
                            {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                          </span>
                        </div>
                        {slot.classroom && (
                          <div className="flex min-w-0 items-start gap-1 text-xs wrap-anywhere text-muted-foreground">
                            <MapPin className="mt-0.5 size-3 shrink-0" />
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
