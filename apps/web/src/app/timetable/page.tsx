"use client";

import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { timetableApi } from '@/lib/api/timetable';
// Header removed (Global layout usage)
import { TimetableSelect } from '@/components/features/timetable/timetable-select';
import { TimetableGrid } from '@/components/features/timetable/timetable-grid';
import { toast } from 'sonner';
import { Loader2, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toPng } from 'html-to-image';

export default function TimetablePage() {
  const queryClient = useQueryClient();
  const [selectedTimetableId, setSelectedTimetableId] = useState<number | null>(null);

  const handleExportImage = async () => {
    const element = document.querySelector('.timetable-grid-target');
    if (!element) {
      toast.error('시간표를 찾을 수 없습니다.');
      return;
    }

    try {
      const dataUrl = await toPng(element as HTMLElement, {
        backgroundColor: '#ffffff',
        style: {
          padding: '20px',
        }
      });
      const link = document.createElement('a');
      link.download = `timetable-${new Date().getTime()}.png`;
      link.href = dataUrl;
      link.click();
      toast.success('시간표 이미지가 저장되었습니다.');
    } catch {
      toast.error('이미지 저장에 실패했습니다.');
    }
  };

  // 1. 시간표 목록 조회
  const { data: timetablesData, isLoading: isListLoading } = useQuery({
    queryKey: ['timetables'],
    queryFn: () => timetableApi.getTimetables(),
  });

  const timetables = timetablesData?.data || [];

  // 2. 초기 선택값 결정 (대표 시간표 혹은 첫 번째 시간표)
  useEffect(() => {
    if (timetables.length > 0 && selectedTimetableId === null) {
      const primary = timetables.find((t) => t.primary);
      setSelectedTimetableId(primary ? primary.id : timetables[0].id);
    }
  }, [timetables, selectedTimetableId]);

  // 3. 현재 선택된 시간표 상세 조회
  const { data: timetableDetail, isLoading: isDetailLoading } = useQuery({
    queryKey: ['timetable', selectedTimetableId],
    queryFn: () => timetableApi.getTimetable(selectedTimetableId!),
    enabled: selectedTimetableId !== null,
  });

  // 4. Mutations
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

  // const isLoading = isListLoading || (selectedTimetableId !== null && isDetailLoading);

  return (
    <div className="min-h-screen bg-background">
      
      <main className="container max-w-5xl py-8 space-y-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold tracking-tight">내 시간표</h1>
            <p className="text-muted-foreground">나만의 수강 바구니를 구성하고 최적의 시간표를 만들어보세요.</p>
          </div>
          
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" className="gap-2" onClick={handleExportImage}>
              <Download className="w-4 h-4" />
              이미지로 저장
            </Button>
          </div>
        </div>

        <section className="space-y-4">
          {isListLoading ? (
            <div className="flex items-center justify-center py-10">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
            </div>
          ) : (
            <TimetableSelect 
              timetables={timetables}
              currentId={selectedTimetableId || 0}
              onSelect={setSelectedTimetableId}
              onCreate={(name) => createMutation.mutate(name)}
              onDelete={(id) => deleteMutation.mutate(id)}
              onSetPrimary={(id) => setPrimaryMutation.mutate(id)}
              isLoading={createMutation.isPending}
            />
          )}

          {isDetailLoading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
          ) : selectedTimetableId && timetableDetail?.data ? (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 timetable-grid-target">
              <TimetableGrid timetable={timetableDetail.data} />
            </div>
          ) : null}

          {!isListLoading && timetables.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed rounded-xl bg-muted/20">
              <p className="text-muted-foreground mb-4">아직 생성된 시간표가 없습니다.</p>
              <Button onClick={() => createMutation.mutate('기본 시간표')}>
                첫 시간표 만들기
              </Button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
