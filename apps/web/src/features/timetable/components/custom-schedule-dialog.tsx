"use client";

import { useId, useState } from 'react';
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
import { toast } from 'sonner';
import { reportClientError } from '@/shared/telemetry/client-error';

const schema = z.object({
  title: z.string().min(1, '과목명(일정 제목)을 입력해주세요.'),
  professor: z.string().optional(),
  classroom: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const fieldIconClassName = 'pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground';
const fieldInputClassName = 'h-11 rounded-lg bg-muted/30 pl-9 shadow-none';

interface CustomScheduleDialogProps {
  timetableId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CustomScheduleDialog({ timetableId, open, onOpenChange }: CustomScheduleDialogProps) {
  const formId = useId();
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
      toast.error('최소 하나 이상의 시간대를 선택해주세요.');
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
      reportClientError(error, { source: "custom-schedule", operation: "add" });
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleDialogOpenChange}>
      <DialogContent className="max-w-4xl p-0 overflow-hidden border-none bg-white dark:bg-[#251e2b] max-h-[95vh] flex flex-col">
        <DialogHeader className="px-8 py-6 border-b border-gray-100 dark:border-gray-800 bg-white/50 dark:bg-white/5 text-left">
          <DialogTitle className="text-2xl font-bold text-gray-900 dark:text-white tracking-tight">일정 직접 추가</DialogTitle>
          <DialogDescription className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            일정 정보를 입력하고 시간을 선택하세요.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="flex-1 overflow-y-auto">
          <div className="px-8 py-6 space-y-8">
            <div className="grid gap-x-4 gap-y-4 min-[480px]:grid-cols-2">
              <div className="col-span-full grid gap-2">
                <Label htmlFor={`${formId}-title`}>
                  일정 제목 <span aria-hidden="true" className="text-primary">*</span>
                </Label>
                <div className="relative">
                  <Edit2 aria-hidden="true" className={fieldIconClassName} />
                  <Input
                    {...register('title')}
                    id={`${formId}-title`}
                    aria-required="true"
                    aria-invalid={Boolean(errors.title)}
                    aria-describedby={errors.title ? `${formId}-title-error` : undefined}
                    className={fieldInputClassName}
                    placeholder="예: 전공 공부"
                  />
                </div>
                {errors.title && <p id={`${formId}-title-error`} className="text-xs text-destructive">{errors.title.message}</p>}
              </div>

              <div className="grid gap-2">
                <Label htmlFor={`${formId}-professor`}>교수명/설명</Label>
                <div className="relative">
                  <User aria-hidden="true" className={fieldIconClassName} />
                  <Input
                    {...register('professor')}
                    id={`${formId}-professor`}
                    className={fieldInputClassName}
                    placeholder="예: 홍길동 교수님"
                  />
                </div>
              </div>

              <div className="grid gap-2">
                <Label htmlFor={`${formId}-classroom`}>장소/강의실</Label>
                <div className="relative">
                  <MapPin aria-hidden="true" className={fieldIconClassName} />
                  <Input
                    {...register('classroom')}
                    id={`${formId}-classroom`}
                    className={fieldInputClassName}
                    placeholder="예: 중앙도서관"
                  />
                </div>
              </div>
            </div>

            <hr className="border-gray-100 dark:border-gray-800" />

            <div className="space-y-3">
              <h3 id={`${formId}-time-heading`} className="flex items-center gap-2 text-base font-bold text-foreground">
                <Clock aria-hidden="true" className="size-5 text-primary" />
                시간 선택
              </h3>

              <div role="group" aria-labelledby={`${formId}-time-heading`} aria-describedby={`${formId}-time-help`} className="rounded-2xl border border-gray-100 dark:border-gray-800 p-2 bg-gray-50/30 dark:bg-black/10">
                <TimeTableSelector
                  selected={selectedSchedules}
                  onChange={setSelectedSchedules}
                />
              </div>
              <p id={`${formId}-time-help`} className="text-xs text-muted-foreground">
                드래그하여 여러 칸을 한 번에 선택할 수 있습니다.
              </p>
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
              추가하기
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
