"use client";

import { useEffect, useRef, useState } from "react";
import { Bell, CalendarDays, Check, Search } from "lucide-react";

import { Badge } from "@/shared/ui/badge";

import styles from "./landing-walkthrough.module.css";

const sampleCourse = {
  name: "자료구조",
  section: "01분반",
  credits: "3학점",
  meetingTime: "월·수 10:00–11:15",
};

function SearchPreview() {
  return (
    <div className="grid gap-3">
      <div className="flex items-center gap-2 rounded-lg border bg-card px-3 py-2.5 text-sm font-semibold">
        <Search className="size-4 text-muted-foreground" />
        {sampleCourse.name}
      </div>
      <div className="flex flex-wrap gap-1.5">
        <Badge variant="outline" className="bg-card">전공필수</Badge>
        <Badge variant="outline" className="bg-card">{sampleCourse.credits}</Badge>
      </div>
      <ul className="grid gap-2">
        <li data-preview-focus className="flex items-center justify-between gap-2 rounded-xl border border-primary/25 bg-primary/[0.06] px-3 py-2.5">
          <div className="min-w-0">
            <p className="text-sm font-bold">{sampleCourse.name} <span className="text-xs text-primary">{sampleCourse.section}</span></p>
            <p className="mt-0.5 text-xs text-muted-foreground">{sampleCourse.meetingTime}</p>
          </div>
          <span className="shrink-0 text-xs font-bold text-muted-foreground">여석 0</span>
        </li>
        <li className="flex items-center justify-between gap-2 rounded-xl border bg-card px-3 py-2.5">
          <div className="min-w-0">
            <p className="text-sm font-semibold">{sampleCourse.name} <span className="text-xs text-muted-foreground">02분반</span></p>
            <p className="mt-0.5 text-xs text-muted-foreground">화·목 13:00–14:15</p>
          </div>
          <span className="shrink-0 text-xs font-bold text-primary">여석 2</span>
        </li>
      </ul>
    </div>
  );
}

function NotificationPreview() {
  return (
    <div className="grid gap-4">
      <p className="flex items-center gap-2 px-1 text-xs font-bold text-primary">
        <Check className="size-4" />
        {sampleCourse.name} {sampleCourse.section} 알림 구독 중
      </p>
      <div data-preview-focus className="rounded-xl border border-primary/20 bg-card p-3 sm:p-4">
        <div className="flex items-center gap-2.5">
          <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-primary text-primary-foreground">
            <Bell className="size-4" />
          </span>
          <p className="min-w-0 flex-1 text-xs font-bold text-muted-foreground">줍줍 웹 푸시</p>
          <span className="text-xs text-muted-foreground">방금</span>
        </div>
        <p className="mt-3 text-base font-bold">수강신청 빈자리가 생겼어요!</p>
        <p className="mt-1 text-sm text-muted-foreground">{sampleCourse.name} {sampleCourse.section}</p>
        <Badge className="mt-3 bg-primary/10 text-primary">여석 1</Badge>
      </div>
    </div>
  );
}

function TimetablePreview() {
  return (
    <div className="grid gap-3">
      <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
        <p className="text-sm font-bold">내 시간표</p>
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
          <span>총 9학점</span>
          <span className="flex items-center gap-0.5 text-primary"><Check className="size-3" /> 시간 겹침 없음</span>
        </p>
      </div>
      <div className={styles.timetable}>
        {["", "월", "화", "수", "목", "금"].map((day) => <span key={day} className={styles.day}>{day}</span>)}
        {["09", "10", "11", "12"].map((hour, index) => (
          <span key={hour} className={styles.hour} style={{ gridRow: index + 2 }}>{hour}</span>
        ))}
        <div data-preview-focus className={styles.courseBlock} style={{ gridColumn: 2 }}>{sampleCourse.name}</div>
        <div data-preview-focus className={styles.courseBlock} style={{ gridColumn: 4 }}>{sampleCourse.name}</div>
        <div className={styles.otherBlock} style={{ gridColumn: 3, gridRow: 2 }}>이산수학</div>
        <div className={styles.otherBlock} style={{ gridColumn: 5, gridRow: 5 }}>운영체제</div>
      </div>
      <p className="text-xs font-semibold text-primary">{sampleCourse.name} {sampleCourse.section} · {sampleCourse.meetingTime}</p>
    </div>
  );
}

const chapters = [
  {
    id: "search",
    label: "강의 검색",
    title: "정밀 강의 검색",
    description: "강의명과 교수님으로 찾아보세요. 이수 구분, 학점, 강의 시간대까지 좁혀 원하는 분반의 여석을 확인할 수 있어요.",
    Icon: Search,
    Preview: SearchPreview,
  },
  {
    id: "notification",
    label: "여석 알림",
    title: "실시간 여석 알림",
    description: "듣고 싶은 강의가 꽉 찼다면 알림을 신청하세요. 빈자리가 생기는 즉시 웹 푸시로 알려드려요.",
    Icon: Bell,
    Preview: NotificationPreview,
  },
  {
    id: "timetable",
    label: "시간표",
    title: "스마트 시간표 관리",
    description: "담아 둔 강의로 시간표를 짜 보세요. 수업 시간이 겹치는지, 총 몇 학점인지 바로 확인할 수 있어요.",
    Icon: CalendarDays,
    Preview: TimetablePreview,
  },
] as const;

type ChapterId = (typeof chapters)[number]["id"];

export function LandingWalkthrough() {
  const chapterListRef = useRef<HTMLOListElement>(null);
  const [activeChapter, setActiveChapter] = useState<ChapterId | null>(null);

  useEffect(() => {
    const list = chapterListRef.current;
    if (!list) return;

    let frame = 0;
    const updateChapter = () => {
      frame = 0;
      if (getComputedStyle(list).getPropertyValue("--sticky-preview").trim() !== "1") {
        setActiveChapter(null);
        return;
      }

      const readingLine = window.innerHeight * 0.52;
      let nextChapter: ChapterId = chapters[0].id;
      for (const [index, chapter] of chapters.entries()) {
        const item = list.children.item(index);
        if (!item || item.getBoundingClientRect().top > readingLine) break;
        nextChapter = chapter.id;
      }
      setActiveChapter(nextChapter);
    };

    const scheduleUpdate = () => {
      if (!frame) frame = window.requestAnimationFrame(updateChapter);
    };
    const resizeObserver = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(scheduleUpdate);
    resizeObserver?.observe(list);
    window.addEventListener("scroll", scheduleUpdate, { passive: true });
    window.addEventListener("resize", scheduleUpdate);
    scheduleUpdate();

    return () => {
      resizeObserver?.disconnect();
      window.removeEventListener("scroll", scheduleUpdate);
      window.removeEventListener("resize", scheduleUpdate);
      window.cancelAnimationFrame(frame);
    };
  }, []);

  const activeIndex = chapters.findIndex(({ id }) => id === activeChapter);

  return (
    <section aria-labelledby="landing-walkthrough-title" className="mx-auto w-full max-w-[1200px] px-6 pb-20 md:pb-32">
      <div className="mb-8 border-t pt-10 lg:mb-10 lg:pt-12">
        <h2 id="landing-walkthrough-title" className="text-2xl font-bold tracking-tight md:text-3xl">
          줍줍으로 수강신청 준비하기
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground md:text-base">
          한 과목을 골라 빈자리 알림을 받고, 내 시간표에 담는 과정을 살펴보세요.
        </p>
      </div>

      <div className={styles.story} data-active-chapter={activeChapter ?? undefined}>
        <ol ref={chapterListRef} className={styles.chapters}>
          {chapters.map(({ id, title, description, label, Icon, Preview }) => (
            <li key={id} className={styles.chapter} data-current={id === activeChapter || undefined}>
              <div className={styles.copy}>
                <div className={styles.stepMark} aria-hidden="true">
                  <Icon className="size-[18px]" />
                </div>
                <h3 className="mt-3.5 text-xl font-bold tracking-tight md:text-2xl">{title}</h3>
                <p className="mt-2 max-w-sm text-sm leading-relaxed text-muted-foreground md:text-base">{description}</p>
              </div>
              <div className={styles.inlinePreview} aria-hidden="true">
                <div className={styles.frame}>
                  <div className={styles.frameBar}>
                    <span className="flex items-center gap-2"><Icon className="size-4 text-primary" />{label}</span>
                    <span className={styles.example}>화면 예시</span>
                  </div>
                  <div className={styles.scene}><Preview /></div>
                </div>
              </div>
            </li>
          ))}
        </ol>

        <div className={styles.stickyPreview} data-testid="landing-sticky-preview" aria-hidden="true">
          <div className={styles.frame}>
            <div className={styles.frameBar}>
              <span>줍줍 미리보기</span>
              <span className={styles.example}>화면 예시</span>
            </div>
            <div className={styles.scenes}>
              {chapters.map(({ id, label, Icon, Preview }, index) => (
                <div
                  key={id}
                  className={styles.scene}
                  data-position={index === activeIndex ? "current" : index < activeIndex ? "before" : "after"}
                >
                  <p className="mb-3 flex items-center gap-2 text-xs font-bold text-primary">
                    <Icon className="size-4" />
                    {label}
                  </p>
                  <Preview />
                </div>
              ))}
            </div>
            <div className={styles.progress}>
              {chapters.map(({ id }, index) => <span key={id} data-complete={index <= activeIndex || undefined} />)}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
