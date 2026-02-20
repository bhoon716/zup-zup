"use client";

import { useState } from "react";
import { useSubscriptions, useUnsubscribe } from "@/features/subscription/hooks/useSubscriptions";
import { Loader2, Bookmark, Trash2 } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import type { Course, Subscription } from "@/shared/types/api";

/**
 * 사용자가 구독한 강의 목록을 사이드바 형태로 렌더링합니다.
 * 강의 클릭 시 상세 모달을 표시하며, 알림 구독 해제 기능을 제공합니다.
 */
export function SubscriptionList() {
  const { data: subscriptions, isLoading } = useSubscriptions();
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();

  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  /**
   * 구독 정보를 Course 타입으로 매핑하여 상세 다이얼로그를 엽니다.
   */
  const handleCardClick = (sub: Subscription) => {
    const parts = sub.courseKey.split(':');
    const mapped: Course = {
      courseKey: sub.courseKey,
      name: sub.courseName,
      professor: sub.professorName,
      subjectCode: parts[2] || '',
      classNumber: parts[3] || '',
    };
    setSelectedCourse(mapped);
    setIsDialogOpen(true);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Loader2 className="w-6 h-6 animate-spin text-primary" />
      </div>
    );
  }

  if (!subscriptions || subscriptions.length === 0) {
    return (
      <div className="rounded-[1.25rem] border border-dashed border-slate-200 bg-slate-50 p-8 text-center">
        <p className="text-sm text-slate-400">구독 중인 강의가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between mb-1 mt-2">
        <h2 className="text-lg font-bold flex items-center gap-2 text-slate-900">
          <Bookmark className="text-primary w-5 h-5 fill-primary" />
          나의 구독 강의
        </h2>
        <span className="text-[10px] px-2.5 py-1 bg-slate-100 text-slate-500 rounded-lg font-bold">
          {subscriptions.length}개 구독 중
        </span>
      </div>

      <div className="space-y-4">
        {subscriptions.map((sub) => {
          const parts = sub.courseKey.split(':');
          const subjectCode = parts[2] || sub.courseKey;
          const classNumber = parts[3] || '01';

          return (
            <div 
              key={sub.id} 
              className="bg-white rounded-[1.25rem] p-5 shadow-sm border border-slate-100 hover:shadow-md transition-all group relative cursor-pointer"
              onClick={() => handleCardClick(sub)}
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex gap-3">
                  <div>
                    <h3 className="font-bold text-base text-slate-900 leading-tight group-hover:text-primary transition-colors">
                      {sub.courseName}
                    </h3>
                    <div className="flex items-center gap-1.5 mt-1.5">
                      <span className="text-[10px] text-slate-500 font-bold bg-slate-50 px-1.5 py-0.5 rounded">
                        {classNumber}분반
                      </span>
                      <span className="text-[10px] text-slate-400 font-medium">
                        {subjectCode}
                      </span>
                    </div>
                  </div>
                </div>
                
                <button 
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm('이 강의의 알림 구독을 해제하시겠습니까?')) {
                      unsubscribe(sub.id);
                    }
                  }}
                  disabled={isUnsubscribing}
                  className="p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              <div className="flex flex-col gap-2 pt-3 border-t border-slate-50">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] text-slate-500 flex items-center gap-1.5">
                    <span className="w-1 h-1 rounded-full bg-slate-300" />
                    {sub.professorName || '교수 미지정'}
                  </span>
                  <Badge variant="secondary" className="h-5 px-1.5 text-[9px] font-bold rounded-md bg-slate-50 text-slate-500 border-none">
                    알림 활성
                  </Badge>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <CourseDetailDialog
        course={selectedCourse}
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </div>
  );
}
