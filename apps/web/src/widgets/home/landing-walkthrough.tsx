"use client";

import { Bell, BellPlus, CalendarPlus, Check, Clock3, Heart, MapPin, Search, UserRound } from "lucide-react";
import Image from "next/image";
import { useEffect, useRef, type ReactNode, type Ref } from "react";

import { Badge } from "@/shared/ui/badge";

import styles from "./landing-walkthrough.module.css";

const sampleCourse = {
  name: "자료구조",
  professor: "김민준",
  code: "COM2101",
  credits: "3학점",
  capacity: 40,
  sections: [
    { number: "01", time: "월·수 10:00–11:15", room: "공대 7호관 204호", enrolled: 40 },
    { number: "02", time: "화·목 13:00–14:15", room: "공대 7호관 201호", enrolled: 38 },
  ],
};

function SampleCourseCard({ section, subscribed = false }: {
  section: (typeof sampleCourse.sections)[number];
  subscribed?: boolean;
}) {
  const available = sampleCourse.capacity - section.enrolled;
  const actionClass = "grid size-7 place-items-center rounded-md border text-muted-foreground";

  return (
    <div className="grid gap-2 rounded-xl border border-l-[3px] border-l-blue-500 bg-card px-3 py-2.5">
      <div className="flex flex-wrap items-center gap-1.5 text-[11px]">
        <span className="rounded bg-blue-500/10 px-1.5 py-0.5 font-semibold text-blue-700 dark:text-blue-300">전공필수</span>
        <span className="rounded bg-muted px-1.5 py-0.5">{sampleCourse.credits}</span>
        <span className="text-muted-foreground">{sampleCourse.code}-{section.number}</span>
      </div>
      <div>
        <div className="flex flex-wrap items-center justify-between gap-x-2 gap-y-1">
          <p className="text-sm font-bold">
            {sampleCourse.name} <span className="text-xs font-medium text-muted-foreground">{section.number}분반</span>
          </p>
          <Badge variant="outline" className={available ? "bg-amber-500/10 text-amber-800 dark:text-amber-200" : "bg-red-500/10 text-red-700 dark:text-red-300"}>
            {available ? "마감 임박" : "마감"}
          </Badge>
        </div>
        <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1"><UserRound className="size-3 shrink-0" />{sampleCourse.professor}</span>
          <span className="inline-flex items-center gap-1"><Clock3 className="size-3 shrink-0" />{section.time}</span>
          <span className="inline-flex items-center gap-1"><MapPin className="size-3 shrink-0" />{section.room}</span>
        </div>
      </div>
      <div className="flex items-end gap-3 border-t pt-2">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap justify-between gap-x-2 text-[11px] text-muted-foreground">
            <span>신청 / 정원 <strong className="text-foreground">{section.enrolled} / {sampleCourse.capacity}</strong></span>
            <span className={available ? "font-semibold text-primary" : undefined}>여석 {available}</span>
          </div>
          <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-muted">
            <div
              className={available ? "h-full rounded-full bg-amber-500" : "h-full rounded-full bg-red-500"}
              style={{ width: `${(section.enrolled / sampleCourse.capacity) * 100}%` }}
            />
          </div>
        </div>
        <div className="flex shrink-0 gap-1">
          <span className={actionClass}><CalendarPlus className="size-3.5" /></span>
          <span className={actionClass}><Heart className="size-3.5" /></span>
          <span className={subscribed ? "grid size-7 place-items-center rounded-md bg-primary text-primary-foreground" : actionClass}>
            <Bell className={subscribed ? "size-3.5 fill-current" : "size-3.5"} />
          </span>
        </div>
      </div>
    </div>
  );
}

function SearchPreview({ registered = false }: { registered?: boolean }) {
  return (
    <div className="grid gap-2.5">
      {registered ? (
        <div className="flex min-h-[34px] flex-wrap items-center gap-x-2 gap-y-0.5 rounded-lg border border-primary/15 bg-card px-3 py-1.5 text-xs">
          <span className="inline-flex items-center gap-1.5 font-bold text-primary">
            <Check className="size-3.5" /> 등록 완료
          </span>
          <span className="text-muted-foreground">빈자리가 생기면 웹 푸시로 알려드려요.</span>
        </div>
      ) : (
        <div className="flex min-h-[34px] items-center gap-2 rounded-lg border bg-card py-1 pr-1 pl-3 text-sm font-semibold">
          <Search className="size-4 shrink-0 text-muted-foreground" />
          <span className="flex-1">{sampleCourse.name}</span>
          <span className="rounded-md bg-primary px-2.5 py-1 text-xs text-primary-foreground">검색</span>
        </div>
      )}
      {sampleCourse.sections.map((section, index) => (
        <SampleCourseCard key={section.number} section={section} subscribed={registered && index === 0} />
      ))}
    </div>
  );
}

function RegistrationPreview() {
  return <SearchPreview registered />;
}

function NotificationPreview() {
  const [section] = sampleCourse.sections;

  return (
    <div className="grid content-center gap-2.5">
      <div className="rounded-2xl border bg-card p-3.5 shadow-sm">
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <Image src="/icons/zup-zup-192.png" alt="" width={24} height={24} className="rounded-md" />
          <span className="flex-1 font-semibold">줍줍</span>
          <span>방금</span>
        </div>
        <p className="mt-2 text-sm font-bold">[줍줍] {sampleCourse.name} ({sampleCourse.professor}) 여석 발생!</p>
        <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
          과목코드: {sampleCourse.code}-{section.number}
          <br />
          현재 여석이 발생했습니다! (1명)
        </p>
      </div>
      <SampleCourseCard section={{ ...section, enrolled: sampleCourse.capacity - 1 }} subscribed />
    </div>
  );
}

const chapters = [
  {
    id: "search",
    title: "강의 검색",
    description: "강의명이나 교수님 이름으로 검색하고, 원하는 분반에 자리가 남았는지 확인해 보세요.",
    Icon: Search,
    Preview: SearchPreview,
  },
  {
    id: "registration",
    title: "빈자리 알림 등록",
    description: "듣고 싶은 분반이 꽉 찼다면 검색 결과에서 종 아이콘을 눌러 알림을 신청하세요.",
    Icon: BellPlus,
    Preview: RegistrationPreview,
  },
  {
    id: "notification",
    title: "빈자리 알림",
    description: "자리가 나면 웹 푸시로 알려드려요. 알림을 확인하고 학교 수강신청 페이지에서 신청하세요.",
    Icon: Bell,
    Preview: NotificationPreview,
  },
] as const;

function WalkthroughPreview({ activeIndex, children, stickyRef }: {
  activeIndex: number;
  children: ReactNode;
  stickyRef?: Ref<HTMLDivElement>;
}) {
  return (
    <div
      ref={stickyRef}
      className={`${styles.frame} ${stickyRef ? styles.stickyPreview : styles.inlinePreview}`}
      data-testid={stickyRef ? "landing-walkthrough-sticky-preview" : "landing-walkthrough-preview"}
      aria-hidden="true"
    >
      <div className={styles.frameBar}>
        <span>줍줍 미리보기</span>
        <span className={styles.example}>화면 예시</span>
      </div>
      {children}
      <div className={styles.progress}>
        {chapters.map(({ id }, index) => <span key={id} data-complete={index <= activeIndex || undefined} />)}
      </div>
    </div>
  );
}

export function LandingWalkthrough() {
  const stageRef = useRef<HTMLDivElement>(null);
  const stickyRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const stage = stageRef.current;
    const preview = stickyRef.current;
    if (!stage || !preview) return;

    const desktop = window.matchMedia("(min-width: 48rem) and (min-height: 40rem) and (prefers-reduced-motion: no-preference)");
    const chapterItems = stage.querySelectorAll<HTMLElement>(`.${styles.chapter}`);
    const scenes = preview.querySelectorAll<HTMLElement>(`.${styles.stickyScene}`);
    const progressIndicators = preview.querySelectorAll(`.${styles.progress} > span`);
    const minimumStickyDistance = 240;
    const fadePortion = 0.4;
    const holdPortion = (1 - fadePortion) / 2;
    let frame: number | undefined;
    let range: { start: number; distance: number } | undefined;
    let needsMeasure = true;
    let lastProgress: number | undefined;
    let lastActiveIndex: number | undefined;

    const measureRange = () => {
      if (!desktop.matches) {
        stage.removeAttribute("data-sticky");
        return;
      }

      stage.setAttribute("data-sticky", "");
      const { top, height } = stage.getBoundingClientRect();
      const inset = Number.parseFloat(window.getComputedStyle(preview).top);
      const { clientHeight, scrollHeight } = document.documentElement;
      const previewHeight = preview.getBoundingClientRect().height;
      const stickyStart = window.scrollY + top - inset;
      const stickyEnd = stickyStart + height - previewHeight;
      const start = Math.max(0, stickyStart);
      const end = Math.min(stickyEnd, scrollHeight - clientHeight);
      const distance = end - start;
      if (distance < minimumStickyDistance || inset + previewHeight > clientHeight) {
        stage.removeAttribute("data-sticky");
        return;
      }

      return { start, distance };
    };

    const updatePreview = () => {
      frame = undefined;
      if (needsMeasure) {
        needsMeasure = false;
        range = measureRange();
      }
      if (!range) {
        if (lastActiveIndex !== undefined) chapterItems[lastActiveIndex]?.removeAttribute("aria-current");
        lastActiveIndex = undefined;
        lastProgress = undefined;
        return;
      }

      const position = Math.min(1, Math.max(0, (window.scrollY - range.start) / range.distance));
      const travel = position * (chapters.length - 1);
      const from = Math.min(chapters.length - 2, Math.floor(travel));
      const blend = Math.min(1, Math.max(0, (travel - from - holdPortion) / fadePortion));
      const progress = from + blend;
      if (progress === lastProgress) return;

      lastProgress = progress;
      scenes.forEach((scene, index) => {
        const emphasis = String(Math.max(0, 1 - Math.abs(index - progress)));
        scene.style.opacity = emphasis;
        chapterItems[index]?.style.setProperty("--chapter-emphasis", emphasis);
      });

      const activeIndex = Math.round(progress);
      if (activeIndex === lastActiveIndex) return;

      if (lastActiveIndex !== undefined) chapterItems[lastActiveIndex]?.removeAttribute("aria-current");
      chapterItems[activeIndex]?.setAttribute("aria-current", "step");
      lastActiveIndex = activeIndex;
      progressIndicators.forEach((indicator, index) => {
        indicator.toggleAttribute("data-complete", index <= activeIndex);
      });
    };

    const scheduleUpdate = () => {
      if (!needsMeasure && !range) return;
      frame ??= window.requestAnimationFrame(updatePreview);
    };
    const scheduleMeasure = () => {
      needsMeasure = true;
      scheduleUpdate();
    };

    const resizeObserver = new ResizeObserver(scheduleMeasure);
    resizeObserver.observe(stage);
    resizeObserver.observe(preview);
    resizeObserver.observe(document.body);
    window.addEventListener("scroll", scheduleUpdate, { passive: true });
    window.addEventListener("resize", scheduleMeasure);
    desktop.addEventListener("change", scheduleMeasure);
    scheduleMeasure();

    return () => {
      resizeObserver.disconnect();
      window.removeEventListener("scroll", scheduleUpdate);
      window.removeEventListener("resize", scheduleMeasure);
      desktop.removeEventListener("change", scheduleMeasure);
      if (frame !== undefined) window.cancelAnimationFrame(frame);
    };
  }, []);

  return (
    <section aria-labelledby="landing-walkthrough-title" className="mx-auto w-full max-w-[1200px] px-6 pb-10 md:pb-12">
      <div className="mb-5 border-t pt-8 lg:mb-6">
        <h2 id="landing-walkthrough-title" className="text-2xl font-bold tracking-tight md:text-3xl">
          줍줍으로 수강신청 준비하기
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground md:text-base">
          분반별 여석을 확인하고, 마감된 분반에는 빈자리 알림을 등록하세요.
        </p>
      </div>
      <div ref={stageRef} className={styles.stage} data-testid="landing-walkthrough-stage">
        <ol className={styles.chapterList} role="list">
          {chapters.map(({ id, title, description, Icon, Preview }, index) => (
            <li key={id} className={styles.chapter}>
              <div className={styles.copy} data-testid="landing-walkthrough-copy">
                <div className={styles.stepMark} aria-hidden="true">
                  <Icon className="size-[18px]" />
                </div>
                <h3 className="mt-3 text-xl font-bold tracking-tight md:text-2xl">{title}</h3>
                <p className="mt-2 max-w-sm break-keep text-sm leading-relaxed text-muted-foreground md:text-base">{description}</p>
              </div>

              <WalkthroughPreview activeIndex={index}>
                <div className={styles.scene}><Preview /></div>
              </WalkthroughPreview>
            </li>
          ))}
        </ol>

        <WalkthroughPreview activeIndex={0} stickyRef={stickyRef}>
          <div className={styles.sceneStack}>
            {chapters.map(({ id, Preview }) => (
              <div key={id} className={`${styles.scene} ${styles.stickyScene}`} data-chapter={id} data-testid="landing-walkthrough-scene">
                <Preview />
              </div>
            ))}
          </div>
        </WalkthroughPreview>
      </div>
    </section>
  );
}
