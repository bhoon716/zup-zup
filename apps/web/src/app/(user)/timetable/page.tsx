"use client";

import React, { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { timetableApi } from '@/features/timetable/api/timetable.api';
import { useTimetables, useTimetableDetail } from '@/features/timetable/hooks/useTimetable';
import { TimetableSelect } from '@/features/timetable/components/timetable-select';
import { TimetableGrid } from '@/features/timetable/components/timetable-grid';
import { useWishlist } from '@/features/wishlist/hooks/useWishlist';
import { toast } from 'sonner';
import { Loader2, Download, CalendarDays, Clock3, Heart, BookOpen, Plus } from 'lucide-react';
import { CreditStatsCard } from '@/features/timetable/components/credit-stats-card';
import { CustomScheduleDialog } from '@/features/timetable/components/custom-schedule-dialog';
import { Button } from '@/shared/ui/button';
import { Badge } from '@/shared/ui/badge';
import { cn } from '@/shared/lib/utils';
import { toPng } from 'html-to-image';

const DAY_MAP: Record<string, string> = {
  MONDAY: '월',
  TUESDAY: '화',
  WEDNESDAY: '수',
  THURSDAY: '목',
  FRIDAY: '금',
  SATURDAY: '토',
  SUNDAY: '일',
  MO: '월',
  TU: '화',
  WE: '수',
  TH: '목',
  FR: '금',
  SA: '토',
  SU: '일',
};

const normalizeDay = (day: string) => DAY_MAP[day] || day;
const TODAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'] as const;

export default function TimetablePage() {
  const queryClient = useQueryClient();
  const [selectedTimetableId, setSelectedTimetableId] = useState<number | null>(null);
  const [sidebarTab, setSidebarTab] = useState<'schedule' | 'wishlist'>('schedule');
  const [customDialogOpen, setCustomDialogOpen] = useState(false);

  /**
   * 현재 시간표 그리드 영역을 PNG 이미지로 캡처하여 다운로드합니다.
   */
  const handleExportImage = async () => {
    const element = document.querySelector('.timetable-grid-content') as HTMLElement;
    if (!element) {
      toast.error('시간표를 찾을 수 없습니다.');
      return;
    }

    try {
      const padding = 40;
      const width = element.scrollWidth + (padding * 2);
      const height = element.scrollHeight + (padding * 2);

      const dataUrl = await toPng(element, {
        backgroundColor: '#ffffff',
        cacheBust: true,
        width: width,
        height: height,
        style: {
          padding: `${padding}px`,
          borderRadius: '0',
          margin: '0',
          transform: 'none',
          width: `${element.scrollWidth}px`,
          height: `${element.scrollHeight}px`,
          boxSizing: 'content-box',
          maxWidth: 'none',
          minWidth: 'none',
        },
        pixelRatio: 2,
      });

      const link = document.createElement('a');
      link.download = `timetable-${new Date().getTime()}.png`;
      link.href = dataUrl;
      link.click();
      toast.success('시간표 이미지가 저장되었습니다.');
    } catch (error) {
      console.error('Export failed:', error);
      toast.error('이미지 저장에 실패했습니다.');
    }
  };

  const { data: timetablesData, isLoading: isListLoading } = useTimetables();
  const timetables = useMemo(() => timetablesData ?? [], [timetablesData]);
  const { data: wishlistData, isLoading: isWishlistLoading } = useWishlist();
  const wishlist = wishlistData ?? [];

  /**
   * 현재 선택된 시간표 ID를 계산합니다. (선택된 ID -> 대표 시간표 -> 첫 번째 시간표 순)
   */
  const activeTimetableId = useMemo(() => {
    if (selectedTimetableId !== null) {
      return selectedTimetableId;
    }
    if (timetables.length === 0) {
      return null;
    }
    const primary = timetables.find((t) => t.primary);
    return primary ? primary.id : timetables[0].id;
  }, [selectedTimetableId, timetables]);

  const { data: timetableDetail, isLoading: isDetailLoading } = useTimetableDetail(activeTimetableId);
  const todayLabel = TODAY_LABELS[new Date().getDay()];

  const createMutation = useMutation({
    mutationFn: (name: string) => timetableApi.createTimetable({ name, primary: timetables.length === 0 }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['timetables'] });
      setSelectedTimetableId(res.data.id);
      toast.success('시간표가 생성되었습니다.');
    },
    onError: () => toast.error('시간표 생성에 실패했습니다.')
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => timetableApi.deleteTimetable(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timetables'] });
      setSelectedTimetableId(null);
      toast.success('시간표가 삭제되었습니다.');
    },
    onError: () => toast.error('시간표 삭제에 실패했습니다.')
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (id: number) => timetableApi.setPrimary(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timetables'] });
      toast.success('대표 시간표로 설정되었습니다.');
    },
    onError: () => toast.error('대표 시간표 설정에 실패했습니다.')
  });

  const timetableName = timetableDetail?.name || '시간표';
  /**
   * 시간표에 포함된 모든 강의의 총 신청 학점을 계산합니다.
   */
  const totalCredits = useMemo(() => {
    if (!timetableDetail) {
      return 0;
    }

    const parsedTotal = Number(timetableDetail.totalCredits);
    if (Number.isFinite(parsedTotal)) {
      return parsedTotal;
    }

    return timetableDetail.courses.reduce((sum, course) => sum + (Number(course.credits) || 0), 0);
  }, [timetableDetail]);

  /**
   * 현재 요일에 해당하는 시간표 및 커스텀 일정 리스트를 필터링하여 정렬합니다.
   */
  const todaySchedules = useMemo(() => {
    if (!timetableDetail) {
      return [];
    }

    const courseItems = timetableDetail.courses.flatMap((course) =>
      (course.schedules ?? [])
        .filter((schedule) => normalizeDay(String(schedule.dayOfWeek)) === todayLabel)
        .map((schedule, idx) => ({
          key: `${course.courseKey}-${idx}`,
          title: course.name,
          subtitle: [course.professor, course.classroom].filter(Boolean).join(' • '),
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          type: 'course' as const,
        }))
    );

    const customItems = (timetableDetail.customSchedules ?? [])
      .filter((schedule) => normalizeDay(schedule.dayOfWeek) === todayLabel)
      .map((schedule) => ({
        key: `custom-${schedule.id}`,
        title: schedule.title,
        subtitle: '직접 추가 일정',
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        type: 'custom' as const,
      }));

    return [...courseItems, ...customItems].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [timetableDetail, todayLabel]);

  const timetableCourses = useMemo(() => {
    if (!timetableDetail) {
      return [];
    }

    return timetableDetail.courses.map((course) => {
      const scheduleText = (course.schedules ?? [])
        .map((schedule) => `${normalizeDay(String(schedule.dayOfWeek))} ${schedule.startTime}-${schedule.endTime}`)
        .join(', ');

      return {
        key: course.courseKey,
        name: course.name,
        professor: course.professor,
        classroom: course.classroom,
        credits: course.credits,
        classification: course.classification,
        scheduleText: scheduleText || course.classTime || '시간 정보 없음',
      };
    });
  }, [timetableDetail]);

  return (
    <div className="min-h-[calc(100vh-4rem)] bg-slate-100">
      <main className="mx-auto flex w-full max-w-[1600px] flex-col gap-4 p-4 lg:flex-row">
        <section className="flex min-h-[calc(100vh-6rem)] min-w-0 flex-1 flex-col overflow-hidden rounded-[1.5rem] border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
              <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">내 시간표</h1>
                <p className="text-sm text-slate-500">강의 시간이 겹치면 대시보드처럼 자동으로 세로 분할되어 표시됩니다.</p>
              </div>

              <div className="flex items-center gap-2">
                <Badge variant="outline" className="rounded-full border-slate-200 px-3 py-1 text-xs font-semibold text-slate-600">
                  <CalendarDays className="h-3.5 w-3.5" />
                  {timetableName}
                </Badge>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-9 rounded-xl border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                  onClick={handleExportImage}
                >
                  <Download className="h-4 w-4" />
                  내보내기
                </Button>
                <Button
                  variant="default"
                  size="sm"
                  className="h-9 rounded-xl bg-primary text-white hover:bg-primary-hover shadow-sm"
                  onClick={() => setCustomDialogOpen(true)}
                >
                  <Plus className="h-4 w-4" />
                  수업 직접 추가
                </Button>
              </div>
            </div>

            {isListLoading ? (
              <div className="mt-4 flex items-center justify-center py-5">
                <Loader2 className="h-6 w-6 animate-spin text-primary" />
              </div>
            ) : (
              <div className="mt-4">
                <TimetableSelect
                  timetables={timetables}
                  currentId={activeTimetableId || 0}
                  onSelect={setSelectedTimetableId}
                  onCreate={(name) => createMutation.mutate(name)}
                  onDelete={(id) => deleteMutation.mutate(id)}
                  onSetPrimary={(id) => setPrimaryMutation.mutate(id)}
                  isLoading={createMutation.isPending}
                />
              </div>
            )}
          </div>

          <div className="min-h-0 flex-1 bg-slate-50/80 p-3 sm:p-4">
            {isDetailLoading ? (
              <div className="flex h-full items-center justify-center py-20">
                <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
              </div>
            ) : activeTimetableId && timetableDetail ? (
              <div className="h-full overflow-auto rounded-2xl border border-slate-200 bg-white p-3">
                <TimetableGrid timetable={timetableDetail} className="mx-auto min-w-[760px] lg:min-w-0" />
              </div>
            ) : null}

            {!isListLoading && timetables.length === 0 && (
              <div className="flex h-full flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-white">
                <p className="mb-4 text-sm text-slate-500">아직 생성된 시간표가 없습니다.</p>
                <Button onClick={() => createMutation.mutate('기본 시간표')}>
                  첫 시간표 만들기
                </Button>
              </div>
            )}
          </div>
        </section>

        <aside className="w-full lg:w-[340px]">
          <div className="flex h-full min-h-[calc(100vh-6rem)] flex-col rounded-[1.5rem] border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 p-5">
              <CreditStatsCard totalCredits={totalCredits} />
            </div>

            <div className="border-b border-slate-100 p-4">
              <div className="grid grid-cols-2 rounded-xl bg-slate-100 p-1">
                <button
                  type="button"
                  className={cn(
                    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    sidebarTab === 'schedule'
                      ? 'bg-primary text-white shadow-sm'
                      : 'text-slate-500 hover:text-slate-700'
                  )}
                  onClick={() => setSidebarTab('schedule')}
                >
                  시간표
                </button>
                <button
                  type="button"
                  className={cn(
                    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                    sidebarTab === 'wishlist'
                      ? 'bg-primary text-white shadow-sm'
                      : 'text-slate-500 hover:text-slate-700'
                  )}
                  onClick={() => setSidebarTab('wishlist')}
                >
                  찜 목록
                </button>
              </div>
            </div>

            <div className="min-h-0 flex-1 space-y-4 overflow-y-auto p-4">
              {sidebarTab === 'schedule' ? (
                <>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div className="mb-3 flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-sm font-bold text-slate-800">
                        <Clock3 className="h-4 w-4 text-primary" />
                        오늘 일정
                      </div>
                      <Badge variant="outline" className="rounded-full border-slate-200 bg-white text-xs text-slate-600">
                        {todayLabel}요일
                      </Badge>
                    </div>
                    {todaySchedules.length === 0 ? (
                      <p className="text-xs text-slate-500">오늘 등록된 일정이 없습니다.</p>
                    ) : (
                      <div className="space-y-2">
                        {todaySchedules.map((item) => (
                          <div key={item.key} className="rounded-xl border border-slate-200 bg-white p-3">
                            <p className="text-sm font-bold text-slate-800">{item.title}</p>
                            <p className="mt-1 text-xs text-slate-500">{item.subtitle}</p>
                            <p className="mt-2 text-xs font-semibold text-primary">
                              {item.startTime} - {item.endTime}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div>
                    <div className="mb-2 flex items-center gap-1.5 px-1 text-xs font-bold text-slate-500">
                      <BookOpen className="h-3.5 w-3.5" />
                      시간표 강의 {timetableCourses.length}건
                    </div>
                    <div className="space-y-2">
                      {timetableCourses.length === 0 ? (
                        <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-4 text-center text-xs text-slate-500">
                          강의가 아직 없습니다.
                        </div>
                      ) : (
                        timetableCourses.map((course) => (
                          <div key={course.key} className="rounded-xl border border-slate-200 bg-white p-3">
                            <div className="flex items-start justify-between gap-2">
                              <p className="text-sm font-bold text-slate-800">{course.name}</p>
                              <Badge variant="secondary" className="h-5 rounded-md px-1.5 text-[10px] font-semibold">
                                {course.credits}학점
                              </Badge>
                            </div>
                            <p className="mt-1 text-xs text-slate-500">{course.professor}</p>
                            <p className="mt-1 text-[11px] text-slate-500">{course.scheduleText}</p>
                            <div className="mt-2 flex items-center gap-1.5 text-[11px] text-slate-500">
                              <span>{course.classroom || '강의실 미정'}</span>
                              {course.classification ? <span>• {course.classification}</span> : null}
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className="mb-2 flex items-center gap-1.5 px-1 text-xs font-bold text-slate-500">
                    <Heart className="h-3.5 w-3.5" />
                    찜한 강의 {wishlist.length}건
                  </div>
                  {isWishlistLoading ? (
                    <div className="flex items-center justify-center py-6">
                      <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
                    </div>
                  ) : wishlist.length === 0 ? (
                    <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-4 text-center text-xs text-slate-500">
                      찜한 강의가 없습니다.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {wishlist.map((course) => (
                        <div key={course.id} className="rounded-xl border border-slate-200 bg-white p-3">
                          <p className="text-sm font-bold text-slate-800">{course.courseName}</p>
                          <p className="mt-1 text-xs text-slate-500">{course.professor}</p>
                          <p className="mt-1 text-[11px] text-slate-500">{course.classTime}</p>
                          <div className="mt-2 flex items-center gap-1.5 text-[11px] text-slate-500">
                            <span>{course.credits}학점</span>
                            <span>• {course.classification}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </aside>
      </main>

      <CustomScheduleDialog 
        timetableId={activeTimetableId}
        open={customDialogOpen}
        onOpenChange={setCustomDialogOpen}
      />
    </div>
  );
}
