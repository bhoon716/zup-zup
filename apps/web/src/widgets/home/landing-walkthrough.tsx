"use client";

import { Bell, BellPlus, Search } from "lucide-react";
import { useEffect, useRef, type ReactNode } from "react";

import {
  SearchPreview,
  RegistrationPreview,
  NotificationPreview,
} from "./landing-walkthrough-previews";
import styles from "./landing-walkthrough.module.css";

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

function WalkthroughPreview({ activeIndex, children, sticky = false }: {
  activeIndex: number;
  children: ReactNode;
  sticky?: boolean;
}) {
  return (
    <div
      className={`${styles.frame} ${sticky ? styles.stickyPreview : styles.inlinePreview}`}
      data-testid={sticky ? "landing-walkthrough-sticky-preview" : "landing-walkthrough-preview"}
      aria-hidden="true"
      inert
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
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const stage = stageRef.current;
    const panel = panelRef.current;
    if (!stage || !panel) return;

    const motion = window.matchMedia("(prefers-reduced-motion: no-preference)");
    const chapterItems = panel.querySelectorAll(`.${styles.chapter}`);
    const scenes = panel.querySelectorAll(`.${styles.stickyScene}`);
    const progressIndicators = panel.querySelectorAll(`.${styles.stickyPreview} .${styles.progress} > span`);
    const minimumStickyDistance = 240;
    const fadePortion = 0.4;
    const holdPortion = (1 - fadePortion) / 2;
    const minimumOpacity = 0.12;
    const fadeTravel = 12;
    let frame: number | undefined;
    let range: { start: number; distance: number } | undefined;
    let needsMeasure = true;
    let lastProgress: number | undefined;
    let lastActiveIndex: number | undefined;

    const measureRange = () => {
      if (!motion.matches) {
        stage.removeAttribute("data-sticky");
        return;
      }

      stage.setAttribute("data-sticky", "");
      const { clientHeight, scrollHeight } = document.documentElement;
      const panelHeight = panel.getBoundingClientRect().height;
      const { top, bottom, height } = stage.getBoundingClientRect();
      const maxScroll = scrollHeight - clientHeight;
      const trackLength = height - panelHeight;
      const earliestInset = window.scrollY + top + trackLength - maxScroll;
      panel.style.setProperty("--sticky-inset-limit", `${Math.floor(clientHeight - panelHeight)}px`);
      panel.style.setProperty("--sticky-inset-minimum", `${Math.ceil(earliestInset)}px`);
      const inset = Number.parseFloat(window.getComputedStyle(panel).top);
      const stickyStart = window.scrollY + top - inset;
      const stickyEnd = window.scrollY + bottom - inset - panelHeight;
      const start = Math.max(0, stickyStart);
      const end = Math.min(stickyEnd, maxScroll);
      const distance = end - start;
      if (distance < minimumStickyDistance || inset + panelHeight > clientHeight) {
        stage.removeAttribute("data-sticky");
        return;
      }

      return { start, distance };
    };

    const updatePanel = () => {
      frame = undefined;
      if (needsMeasure) {
        needsMeasure = false;
        range = measureRange();
      }
      if (!range) {
        if (lastActiveIndex !== undefined) {
          chapterItems[lastActiveIndex]?.removeAttribute("aria-current");
          scenes[lastActiveIndex]?.removeAttribute("data-active");
        }
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
      const activeIndex = Math.round(progress);
      const offset = (activeIndex - progress) * 2;
      const opacity = 1 - (1 - minimumOpacity) * Math.abs(offset);
      panel.style.setProperty("--step-opacity", String(opacity));
      panel.style.setProperty("--step-offset", `${offset * fadeTravel}px`);
      if (activeIndex === lastActiveIndex) return;

      if (lastActiveIndex !== undefined) {
        chapterItems[lastActiveIndex]?.removeAttribute("aria-current");
        scenes[lastActiveIndex]?.removeAttribute("data-active");
      }
      chapterItems[activeIndex]?.setAttribute("aria-current", "step");
      scenes[activeIndex]?.setAttribute("data-active", "");
      lastActiveIndex = activeIndex;
      progressIndicators.forEach((indicator, index) => {
        indicator.toggleAttribute("data-complete", index <= activeIndex);
      });
    };

    const scheduleUpdate = () => {
      if (!needsMeasure && !range) return;
      frame ??= window.requestAnimationFrame(updatePanel);
    };
    const scheduleMeasure = () => {
      needsMeasure = true;
      scheduleUpdate();
    };

    const resizeObserver = new ResizeObserver(scheduleMeasure);
    resizeObserver.observe(stage);
    resizeObserver.observe(panel);
    resizeObserver.observe(document.body);
    window.addEventListener("scroll", scheduleUpdate, { passive: true });
    window.addEventListener("resize", scheduleMeasure);
    motion.addEventListener("change", scheduleMeasure);
    scheduleMeasure();

    return () => {
      resizeObserver.disconnect();
      window.removeEventListener("scroll", scheduleUpdate);
      window.removeEventListener("resize", scheduleMeasure);
      motion.removeEventListener("change", scheduleMeasure);
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
        <div ref={panelRef} className={styles.stickyPanel} data-testid="landing-walkthrough-sticky-panel">
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

          <WalkthroughPreview activeIndex={0} sticky>
            <div className={styles.sceneStack}>
              {chapters.map(({ id, Preview }) => (
                <div key={id} className={`${styles.scene} ${styles.stickyScene}`} data-chapter={id} data-testid="landing-walkthrough-scene">
                  <Preview />
                </div>
              ))}
            </div>
          </WalkthroughPreview>
        </div>
      </div>
    </section>
  );
}
