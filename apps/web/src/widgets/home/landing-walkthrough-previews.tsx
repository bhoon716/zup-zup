import { CalendarPlus, Check, Heart, Search, X } from "lucide-react";
import Image from "next/image";

import {
  CourseResultCard,
  CourseSeatAlertButton,
  courseResultActionBorder,
  courseResultActionSize,
} from "@/features/course/components/course-result-card";
import { normalizeCourse } from "@/shared/lib/course";
import { cn } from "@/shared/lib/utils";
import { BRAND_ICON_192_PATH } from "@/shared/seo/branding";
import type { Course } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";

import styles from "./landing-walkthrough.module.css";

const fullCourse = {
  courseKey: "2026:U211600010:COM2101:01",
  subjectCode: "COM2101",
  stdtrNo: "COM2101-01",
  classNumber: "01",
  name: "자료구조",
  professor: "김민준",
  classroom: "공대 7호관 204호",
  credits: "3",
  classification: "전공필수",
  capacity: 40,
  current: 40,
  averageRating: 4.5,
  reviewCount: 8,
  schedules: [
    { dayOfWeek: "MONDAY", startTime: "10:00", endTime: "11:15" },
    { dayOfWeek: "WEDNESDAY", startTime: "10:00", endTime: "11:15" },
  ],
} satisfies Course;

const otherSection = {
  ...fullCourse,
  courseKey: "2026:U211600010:COM2101:02",
  stdtrNo: "COM2101-02",
  classNumber: "02",
  classroom: "공대 7호관 201호",
  current: 38,
  schedules: [
    { dayOfWeek: "TUESDAY", startTime: "13:00", endTime: "14:15" },
    { dayOfWeek: "THURSDAY", startTime: "13:00", endTime: "14:15" },
  ],
} satisfies Course;

const pushTitle = `[줍줍] ${fullCourse.name} (${fullCourse.professor}) 여석 발생!`;
const pushBody = `과목명: ${fullCourse.name} (${fullCourse.professor})\n과목코드: ${fullCourse.courseKey}\n현재 여석이 발생했습니다! (1명)`;
const subscriptionMessage = "구독이 완료되었습니다";

function ExampleCourseCard({ course, subscribed = false }: { course: Course; subscribed?: boolean }) {
  const actionClass = cn(courseResultActionSize, courseResultActionBorder);

  return (
    <CourseResultCard
      course={normalizeCourse(course)}
      actions={
        <>
          <Button asChild variant="outline" size="icon" className={actionClass}>
            <span><CalendarPlus className="size-4" /></span>
          </Button>
          <Button asChild variant="outline" size="icon" className={actionClass}>
            <span><Heart className="size-4" /></span>
          </Button>
          <CourseSeatAlertButton subscribed={subscribed} />
        </>
      }
    />
  );
}

function ExampleToast({ kind }: { kind: "success" | "push" }) {
  return (
    <div className={styles.toastPreview} data-kind={kind}>
      {kind === "success" ? (
        <>
          <span className={styles.toastIcon}><Check className="size-3" /></span>
          <span className={styles.toastTitle}>{subscriptionMessage}</span>
        </>
      ) : (
        <>
          <span className={styles.toastIcon}>ℹ</span>
          <div className="min-w-0">
            <p className={styles.toastTitle}>{pushTitle}</p>
            <p className={styles.toastBody}>{pushBody}</p>
          </div>
          <span className={styles.toastAction}>보기</span>
        </>
      )}
      <span className={styles.toastClose}><X className="size-3" /></span>
    </div>
  );
}

export function SearchPreview({ registered = false }: { registered?: boolean }) {
  return (
    <div className="relative grid content-start gap-3">
      <div className="flex items-center gap-2">
        <div className="relative min-w-0 flex-1">
          <Input type="search" value={fullCourse.name} readOnly className="h-10 rounded-lg bg-card pl-9" />
          <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        </div>
        <Button asChild className="h-10 rounded-lg"><span>검색</span></Button>
      </div>
      <div className="flex items-center justify-between gap-3">
        <p className="text-lg font-bold tracking-tight">&quot;{fullCourse.name}&quot;</p>
        <span className="rounded-md border border-border/50 bg-muted/30 px-2 py-1 text-xs font-medium text-muted-foreground">
          검색어: {fullCourse.name}
        </span>
      </div>
      <ExampleCourseCard course={fullCourse} subscribed={registered} />
      <div className={styles.secondCourse}>
        <ExampleCourseCard course={otherSection} />
      </div>
      {registered && <ExampleToast kind="success" />}
    </div>
  );
}

export function RegistrationPreview() {
  return <SearchPreview registered />;
}

export function NotificationPreview() {
  return (
    <div className="grid content-center gap-3">
      <div className={styles.systemNotification}>
        <Image src={BRAND_ICON_192_PATH} alt="" width={36} height={36} className="rounded-lg" />
        <div className="min-w-0">
          <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
            <span>줍줍</span>
            <span>지금</span>
          </div>
          <p className="mt-1 text-sm font-semibold">{pushTitle}</p>
          <p className="mt-0.5 text-xs leading-relaxed whitespace-pre-line [overflow-wrap:anywhere] text-muted-foreground">{pushBody}</p>
        </div>
      </div>
      <ExampleToast kind="push" />
    </div>
  );
}
