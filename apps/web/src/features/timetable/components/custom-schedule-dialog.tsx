"use client";

import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Edit2, Clock, Check, MapPin, User } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/shared/ui/dialog';
import { Button } from '@/shared/ui/button';
import { Input } from '@/shared/ui/input';
import { Label } from '@/shared/ui/label';
import { useAddCustomSchedule } from '@/features/timetable/hooks/useTimetable';
import { TimeTableSelector } from '@/features/course/components/time-table-selector';
import { ScheduleCondition } from '@/shared/types/api';
import { toEngDayOfWeek } from '@/shared/lib/formatters';

const schema = z.object({
  title: z.string().min(1, '과목명(일정 제목)을 입력해주세요.'),
  professor: z.string().optional(),
  classroom: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

interface CustomScheduleDialogProps {
  timetableId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * 사용자가 직접 시간표에 일정을 추가할 수 있는 다이얼로그 컴포넌트입니다.
 * 그리드 방식의 시간 선택기(TimeTableSelector)를 사용하여 편리하게 다중 시간대를 선택할 수 있습니다.
 */
export function CustomScheduleDialog({ timetableId, open, onOpenChange }: CustomScheduleDialogProps) {
  const addCustomSchedule = useAddCustomSchedule();
  const [selectedSchedules, setSelectedSchedules] = useState<ScheduleCondition[]>([]);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: '',
      professor: '',
      classroom: '',
    },
  });

  const resetDialogState = () => {
    reset({
      title: '',
      professor: '',
      classroom: '',
    });
    setSelectedSchedules([]);
  };

  const handleDialogOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetDialogState();
    }
    onOpenChange(nextOpen);
  };

  const onSubmit = async (values: FormValues) => {
    if (!timetableId) return;
    if (selectedSchedules.length === 0) {
      alert('최소 하나 이상의 시간대를 선택해주세요.');
      return;
    }

    try {
      await addCustomSchedule.mutateAsync({
        timetableId,
        data: {
          title: values.title,
          professor: values.professor,
          schedules: selectedSchedules.map(s => ({
            dayOfWeek: toEngDayOfWeek(s.dayOfWeek),
            startTime: s.startTime,
            endTime: s.endTime,
            classroom: values.classroom,
          }))
        }
      });
      handleDialogOpenChange(false);
    } catch (error) {
      console.error('Failed to add custom schedules:', error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleDialogOpenChange}>
      <DialogContent className="max-w-4xl p-0 overflow-hidden border-none bg-white dark:bg-[#251e2b] max-h-[95vh] flex flex-col">
        <DialogHeader className="px-8 py-6 border-b border-gray-100 dark:border-gray-800 bg-white/50 dark:bg-white/5 text-left">
          <DialogTitle className="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">일정 직접 추가</DialogTitle>
          <DialogDescription className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            시간표 그리드에서 원하는 칸을 드래그하여 일정을 추가해보세요.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="flex-1 overflow-y-auto">
          <div className="px-8 py-6 space-y-8">
            {/* 기본 정보 섹션 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="space-y-2">
                <Label className="flex items-center gap-2 text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 ml-1">
                  <Edit2 className="h-4 w-4 text-primary" />
                  일정 제목 <span className="text-primary">*</span>
                </Label>
                <Input 
                  {...register('title')}
                  className="w-full h-11 bg-gray-50 dark:bg-white/5 border-gray-200 dark:border-gray-700 rounded-xl px-4 focus:ring-2 focus:ring-primary/50"
                  placeholder="예: 전공 공부"
                />
                {errors.title && <p className="mt-1 ml-1 text-xs text-red-500">{errors.title.message}</p>}
              </div>

              <div className="space-y-2">
                <Label className="flex items-center gap-2 text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 ml-1">
                  <User className="h-4 w-4 text-primary" />
                  교수명/설명
                </Label>
                <Input 
                  {...register('professor')}
                  className="w-full h-11 bg-gray-50 dark:bg-white/5 border-gray-200 dark:border-gray-700 rounded-xl px-4 focus:ring-2 focus:ring-primary/50"
                  placeholder="예: 홍길동 교수님"
                />
              </div>

              <div className="space-y-2">
                <Label className="flex items-center gap-2 text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 ml-1">
                  <MapPin className="h-4 w-4 text-primary" />
                  장소/강의실
                </Label>
                <Input 
                  {...register('classroom')}
                  className="w-full h-11 bg-gray-50 dark:bg-white/5 border-gray-200 dark:border-gray-700 rounded-xl px-4 focus:ring-2 focus:ring-primary/50"
                  placeholder="예: 중앙도서관"
                />
              </div>
            </div>

            <hr className="border-gray-100 dark:border-gray-800" />

            {/* 시간 선택 섹션 */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label className="flex items-center gap-2 text-base font-bold text-gray-800 dark:text-gray-100 ml-1">
                  <Clock className="h-5 w-5 text-primary" />
                  시간 선택
                </Label>
                <div className="text-xs text-gray-500 bg-gray-100 dark:bg-white/5 px-3 py-1 rounded-full">
                  드래그하여 여러 칸을 한 번에 선택할 수 있습니다.
                </div>
              </div>
              
              <div className="rounded-2xl border border-gray-100 dark:border-gray-800 p-2 bg-gray-50/30 dark:bg-black/10">
                <TimeTableSelector 
                  selected={selectedSchedules}
                  onChange={setSelectedSchedules}
                />
              </div>
            </div>
          </div>

          <DialogFooter className="px-8 py-6 bg-gray-50/50 dark:bg-black/10 flex flex-row justify-end items-center gap-4 border-t border-gray-100 dark:border-gray-800 sticky bottom-0 z-20">
            <Button 
              type="button" 
              variant="ghost" 
              onClick={() => handleDialogOpenChange(false)}
              className="px-6 h-12 text-gray-500 font-medium hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200"
            >
              취소
            </Button>
            <Button 
              type="submit"
              disabled={addCustomSchedule.isPending}
              className="bg-primary hover:bg-primary-hover text-white px-10 h-12 rounded-full font-bold shadow-lg shadow-primary/20 transform transition hover:-translate-y-0.5 active:translate-y-0 flex items-center gap-2"
            >
              <Check className="h-5 w-5" />
              시간표에 반영하기
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
