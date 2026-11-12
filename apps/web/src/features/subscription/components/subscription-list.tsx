"use client";

import { useRef, useState } from "react";
import { useSubscriptions, useUnsubscribe, useUnsubscribeAll } from "@/features/subscription/hooks/useSubscriptions";
import { Loader2, Trash2, XCircle } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import { parseCourseKey } from "@/shared/lib/course";
import type { Course, Subscription, User } from "@/shared/types/api";

export function SubscriptionList({ user }: { user: User }) {
  const { data: subscriptions, isLoading } = useSubscriptions(true, user);
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const { mutate: unsubscribeAll, isPending: isUnsubscribingAll } = useUnsubscribeAll();

  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const detailTrigger = useRef<HTMLButtonElement>(null);

  const openCourseDetail = (sub: Subscription, { subjectCode, classNumber }: ReturnType<typeof parseCourseKey>, trigger: HTMLButtonElement) => {
    detailTrigger.current = trigger;
    const mapped: Course = {
      courseKey: sub.courseKey,
      name: sub.courseName,
      professor: sub.professorName,
      subjectCode,
      classNumber,
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
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-600">{subscriptions.length}개 구독 중</p>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => {
            if (confirm("모든 강의의 알림 구독을 해제할까요?")) {
              unsubscribeAll();
            }
          }}
          disabled={isUnsubscribingAll}
          className="text-red-600 hover:bg-red-50 hover:text-red-700"
        >
          <XCircle aria-hidden="true" />
          모두 구독 해제
        </Button>
      </div>

      <div className="space-y-4">
        {subscriptions.map((sub) => {
          const courseKey = parseCourseKey(sub.courseKey);

          return (
            <div 
              key={sub.id} 
              className="bg-white rounded-[1.25rem] p-5 shadow-sm border border-slate-100 hover:shadow-md transition-all group relative cursor-pointer"
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex gap-3">
                  <div>
                    <h3 className="font-bold text-base text-slate-900 leading-tight group-hover:text-primary transition-colors">
                      <button
                        type="button"
                        className="rounded-sm text-left after:absolute after:inset-0 after:content-[''] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                        onClick={(event) => openCourseDetail(sub, courseKey, event.currentTarget)}
                      >
                        {sub.courseName}
                      </button>
                    </h3>
                    <div className="flex items-center gap-1.5 mt-1.5">
                      <span className="text-[10px] text-slate-500 font-bold bg-slate-50 px-1.5 py-0.5 rounded">
                        {courseKey.classNumber || "01"}분반
                      </span>
                      <span className="text-[10px] text-slate-400 font-medium">
                        {courseKey.subjectCode || sub.courseKey}
                      </span>
                    </div>
                  </div>
                </div>
                
                <button
                  type="button"
                  aria-label={`${sub.courseName} 알림 구독 해제`}
                  onClick={() => {
                    if (confirm('이 강의의 알림 구독을 해제할까요?')) {
                      unsubscribe(sub.id);
                    }
                  }}
                  disabled={isUnsubscribing || isUnsubscribingAll}
                  className="relative z-10 p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
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
        returnFocusRef={detailTrigger}
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </div>
  );
}
