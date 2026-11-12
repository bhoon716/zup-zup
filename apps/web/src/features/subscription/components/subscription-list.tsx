"use client";

import { useRef, useState } from "react";
import { useSubscriptions, useUnsubscribe, useUnsubscribeAll } from "@/features/subscription/hooks/useSubscriptions";
import { Loader2, Trash2 } from "lucide-react";
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
      <p role="status" className="flex items-center gap-2 px-4 py-4 text-sm text-muted-foreground">
        <Loader2 aria-hidden="true" className="size-4 animate-spin text-primary" />
        강의 구독을 불러오는 중입니다.
      </p>
    );
  }

  if (!subscriptions || subscriptions.length === 0) {
    return (
      <p className="px-4 py-4 text-sm text-muted-foreground">구독 중인 강의가 없습니다.</p>
    );
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5">
        <p className="text-sm text-muted-foreground">{subscriptions.length}개 구독 중</p>
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
          className="rounded-md text-destructive hover:text-destructive"
        >
          모두 구독 해제
        </Button>
      </div>

      <ul className="divide-y border-t">
        {subscriptions.map((sub) => {
          const courseKey = parseCourseKey(sub.courseKey);

          return (
            <li key={sub.id} className="group relative flex items-start gap-3 px-4 py-3 hover:bg-muted/40 last:rounded-b-xl">
              <div className="min-w-0 flex-1">
                <h3 className="text-sm font-semibold group-hover:text-primary">
                  <button
                    type="button"
                    className="wrap-anywhere rounded-sm text-left after:absolute after:inset-0 after:content-[''] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                    onClick={(event) => openCourseDetail(sub, courseKey, event.currentTarget)}
                  >
                    {sub.courseName}
                  </button>
                </h3>
                <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
                  <span>{courseKey.classNumber || "01"}분반</span>
                  <span>{courseKey.subjectCode || sub.courseKey}</span>
                  <span className="wrap-anywhere">{sub.professorName || "교수 미지정"}</span>
                </div>
              </div>
              <Button
                type="button"
                aria-label={`${sub.courseName} 알림 구독 해제`}
                variant="ghost"
                size="icon-sm"
                onClick={() => {
                  if (confirm("이 강의의 알림 구독을 해제할까요?")) {
                    unsubscribe(sub.id);
                  }
                }}
                disabled={isUnsubscribing || isUnsubscribingAll}
                className="relative z-10 rounded-md text-muted-foreground hover:text-destructive"
              >
                <Trash2 aria-hidden="true" />
              </Button>
            </li>
          );
        })}
      </ul>

      <CourseDetailDialog
        course={selectedCourse}
        returnFocusRef={detailTrigger}
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </>
  );
}
