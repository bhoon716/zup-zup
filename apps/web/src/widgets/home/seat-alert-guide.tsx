import { Bell, CalendarPlus, Check, Heart, Search } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import {
  CourseResultCard,
  courseResultActionBorder,
  courseResultActionSize,
} from "@/features/course/components/course-result-card";
import { normalizeCourse } from "@/shared/lib/course";
import { cn } from "@/shared/lib/utils";
import styles from "./seat-alert-guide.module.css";

const exampleSearchTerm = "자료구조";
const exampleSectionName = `${exampleSearchTerm} 01분반`;
const exampleAvailableSeats = 1;

const steps = [
  {
    id: "search",
    title: "강의 검색하기",
    description: "강의명이나 교수명으로 검색해 원하는 분반을 찾으세요.",
    status: "검색어",
    statusDetail: exampleSearchTerm,
    StatusIcon: Search,
    availableSeats: 0,
    durationMs: 1700,
  },
  {
    id: "subscribe",
    title: "알림 구독 버튼 누르기",
    description: "로그인한 뒤, 원하는 분반 카드에서 알림 구독 버튼을 누르세요.",
    status: "알림 구독 완료",
    statusDetail: exampleSectionName,
    StatusIcon: Check,
    availableSeats: 0,
    durationMs: 1950,
  },
  {
    id: "available",
    title: "빈자리가 나면 알림",
    description: "빈자리가 나면 설정한 채널로 알림이 와요. 수강신청은 학교 시스템에서 직접 하세요.",
    status: "빈자리가 생겼어요!",
    statusDetail: `${exampleSectionName} · 빈자리 ${exampleAvailableSeats}석`,
    StatusIcon: Bell,
    availableSeats: exampleAvailableSeats,
    durationMs: 2350,
  },
] as const;

type Step = (typeof steps)[number];

const firstStep = steps[0];
const finalStep = steps[steps.length - 1];

const exampleCourse = normalizeCourse({
  name: exampleSectionName,
  classification: "전공필수",
  credits: "3",
  stdtrNo: "CS2001",
  professor: "김한별",
  classTime: "월 10:00-11:30, 수 10:00-11:30",
  classroom: "공학관 302",
  capacity: 40,
  current: 40,
});

const exampleActionClass = cn("inline-flex shrink-0 items-center justify-center border", courseResultActionSize);
const STATUS_VISIBILITY_THRESHOLD = 0.9;

export function SeatAlertGuide() {
  const statusRef = useRef<HTMLDivElement>(null);
  const [activeStep, setActiveStep] = useState<Step>(firstStep);
  const course = {
    ...exampleCourse,
    current: exampleCourse.capacity - activeStep.availableSeats,
    available: activeStep.availableSeats,
  };

  useEffect(() => {
    const status = statusRef.current;
    if (!status) return;

    const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (typeof IntersectionObserver === "undefined") {
      const frame = window.requestAnimationFrame(() => setActiveStep(finalStep));
      return () => window.cancelAnimationFrame(frame);
    }

    let latestIntersection: IntersectionObserverEntry | undefined;
    let timer: number | undefined;

    const showStep = (index: number) => {
      const step = steps[index];
      setActiveStep(step);
      timer = window.setTimeout(() => showStep((index + 1) % steps.length), step.durationMs);
    };

    const stop = () => {
      window.clearTimeout(timer);
      timer = undefined;
    };

    const updatePlayback = () => {
      if (motionPreference.matches) {
        stop();
        setActiveStep(finalStep);
        return;
      }

      if (!latestIntersection?.isIntersecting) {
        stop();
        return;
      }

      if (timer === undefined && latestIntersection.intersectionRatio >= STATUS_VISIBILITY_THRESHOLD) {
        showStep(0);
      }
    };

    const observer = new IntersectionObserver((entries) => {
      latestIntersection = entries.at(-1);
      updatePlayback();
    }, { threshold: [0, STATUS_VISIBILITY_THRESHOLD] });

    observer.observe(status);
    motionPreference.addEventListener("change", updatePlayback);
    return () => {
      observer.disconnect();
      observer.takeRecords();
      stop();
      motionPreference.removeEventListener("change", updatePlayback);
    };
  }, []);

  return (
    <section
      aria-labelledby="seat-alert-guide-title"
      data-seat-alert-stage={activeStep.id}
      className="mx-auto w-full max-w-[1200px] px-4 pb-16 sm:px-6 lg:pb-24"
    >
      <div className="grid gap-y-6 rounded-[1.75rem] border border-primary/10 bg-white p-4 sm:gap-y-8 sm:p-8 min-[900px]:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] min-[900px]:gap-x-8 lg:p-10 xl:gap-x-12 xl:p-12 dark:bg-card">
        <h2 id="seat-alert-guide-title" className="break-keep text-2xl font-bold tracking-tight text-foreground sm:text-3xl lg:text-4xl">
          빈자리 알림 받는 법
        </h2>

        <ol role="list">
          {steps.map((step, index) => (
            <li
              key={step.id}
              aria-current={step === activeStep ? "step" : undefined}
              className={cn(styles.step, "relative grid grid-cols-[2.125rem_minmax(0,1fr)] gap-x-3.5 not-last:after:absolute not-last:after:top-[2.125rem] not-last:after:bottom-0 not-last:after:left-[1rem] not-last:after:w-px not-last:after:bg-primary/20 sm:gap-x-4")}
            >
              <span aria-hidden="true" className={cn(styles.stepMarker, "relative z-1 grid size-[2.125rem] place-items-center rounded-full border border-neutral-200 bg-neutral-100 text-sm font-bold tabular-nums text-neutral-600 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-400")}>
                {index + 1}
              </span>
              <div className={cn(styles.stepCopy, "text-neutral-600 dark:text-neutral-400")}>
                <h3 className="pt-1 break-keep text-base font-bold leading-6 sm:text-lg">{step.title}</h3>
                <p className="mt-1 break-keep text-sm leading-relaxed">{step.description}</p>
              </div>
            </li>
          ))}
        </ol>

        <figure
          className={cn(
            styles.example,
            "min-w-0 rounded-[1.25rem] border border-primary/10 bg-white/65 p-2.5 shadow-[0_12px_30px_-20px_#57297055] sm:p-5 min-[900px]:col-start-2 min-[900px]:row-span-2 min-[900px]:row-start-1 min-[900px]:self-center dark:bg-[#f3ecf6]",
          )}
        >
          <figcaption className="mb-3 flex items-center gap-2 px-1 text-sm font-semibold text-primary sm:mb-4">
            <Search aria-hidden="true" className="size-4 shrink-0" />
            강의 검색
            <span className="sr-only">. {exampleSectionName} 카드에서 알림 구독 버튼을 누르는 예시입니다.</span>
          </figcaption>
          <div aria-hidden="true" className="pointer-events-none [&_h2]:break-keep">
            <CourseResultCard
              course={course}
              actions={(
                <>
                  <span className={cn(exampleActionClass, courseResultActionBorder)}>
                    <CalendarPlus className="size-4" />
                  </span>
                  <span className={cn(exampleActionClass, courseResultActionBorder)}>
                    <Heart className="size-4" />
                  </span>
                  <span data-seat-alert-bell className={cn(exampleActionClass, styles.bell)}>
                    <Bell className="size-4" />
                  </span>
                </>
              )}
            />
            <div ref={statusRef} className={cn(styles.statusGrid, "mt-2.5 grid rounded-xl border border-primary/10 bg-[#f6effa] px-2.5 py-2 sm:px-3")}>
              {steps.map((step) => (
                <p
                  key={step.id}
                  data-seat-alert-status={step.id}
                  className={cn(
                    styles.status,
                    step === activeStep && styles.currentStatus,
                    "flex items-center gap-2.5 text-primary",
                  )}
                >
                  <span className={cn(styles.statusIcon, "grid size-6 shrink-0 place-items-center rounded-full bg-primary text-primary-foreground")}>
                    <step.StatusIcon className="size-3.5" strokeWidth={2.5} />
                  </span>
                  <span className="grid min-w-0 break-keep">
                    <span className="text-sm font-semibold">{step.status}</span>
                    <span className="text-xs font-medium text-foreground/75">{step.statusDetail}</span>
                  </span>
                </p>
              ))}
            </div>
          </div>
        </figure>
      </div>
    </section>
  );
}
