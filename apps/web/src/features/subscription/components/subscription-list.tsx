"use client";

import { Loader2, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRef, useState } from "react";
import { useSubscriptions, useUnsubscribe, useUnsubscribeAll } from "@/features/subscription/hooks/useSubscriptions";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import { SEMESTERS } from "@/features/course/constants/course-options";
import { parseCourseKey } from "@/shared/lib/course";
import type { Course, Subscription, User } from "@/shared/types/api";

export function SubscriptionList({ user }: { user: User }) {
  const { data: subscriptions, isLoading, isError, isFetching, refetch } = useSubscriptions(true, user);
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
      <p role="status" className="flex items-center gap-2 p-4 text-sm text-muted-foreground sm:px-5">
        <Loader2 aria-hidden="true" className="size-4 animate-spin text-primary" />
        강의 구독을 불러오는 중입니다.
      </p>
    );
  }

  if (isError && !subscriptions) {
    return (
      <div className="space-y-3 p-4 sm:p-5">
        <p role="alert" className="text-sm text-muted-foreground">강의 구독을 불러오지 못했습니다.</p>
        <Button type="button" variant="outline" disabled={isFetching} onClick={() => void refetch()}>
          다시 불러오기
        </Button>
      </div>
    );
  }

  if (!subscriptions || subscriptions.length === 0) {
    return (
      <div className="p-4 sm:p-5">
        <p className="text-sm font-medium">구독 중인 강의가 없습니다.</p>
        <p className="mt-1 break-keep text-sm text-muted-foreground">강의 검색에서 빈자리 알림을 신청할 수 있습니다.</p>
        <Button asChild variant="outline" className="mt-4">
          <Link href="/search">강의 검색</Link>
        </Button>
      </div>
    );
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 sm:px-5">
        <p className="text-sm tabular-nums text-muted-foreground">{subscriptions.length}개 구독 중</p>
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
          const semesterLabel = SEMESTERS.find(({ value }) => value === courseKey.semester)?.label;

          return (
            <li key={sub.id} className="group relative flex items-center gap-3 px-4 py-3 hover:bg-muted/40 last:rounded-b-xl sm:px-5">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <h3 className="min-w-0 text-sm font-semibold group-hover:text-primary">
                    <button
                      type="button"
                      className="wrap-anywhere rounded-sm text-left after:absolute after:inset-0 after:content-[''] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                      onClick={(event) => openCourseDetail(sub, courseKey, event.currentTarget)}
                    >
                      {sub.courseName}
                    </button>
                  </h3>
                  {semesterLabel && (
                    <Badge variant="secondary" className="rounded-md bg-primary/10 px-1.5 py-0 text-primary">
                      {semesterLabel}
                    </Badge>
                  )}
                </div>
                <p className="mt-1 wrap-anywhere text-xs text-muted-foreground">
                  {courseKey.classNumber || "01"}분반
                  <span className="mx-1">{" · "}</span>
                  {courseKey.subjectCode || sub.courseKey}
                  <span className="mx-1">{" · "}</span>
                  {sub.professorName || "교수 미지정"}
                </p>
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
