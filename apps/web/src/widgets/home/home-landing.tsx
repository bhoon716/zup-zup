import Image from "next/image";
import Link from "next/link";
import { ArrowRight, Bell, CalendarDays, Search } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { LandingMotion } from "./landing-motion";
import styles from "./home-landing.module.css";

const previews = {
  search: {
    caption: "줍줍 강의 검색 화면",
    alt: "검색 필터와 강의별 수강 인원 및 빈자리가 표시된 강의 목록",
    size: [1240, 732],
    mobileSize: [840, 1394],
  },
  notifications: {
    caption: "줍줍 알림함 화면",
    alt: "과목별 빈자리 수와 확인 시각이 최근 순으로 표시된 알림함",
    size: [1050, 620],
    mobileSize: [840, 1478],
  },
  timetable: {
    caption: "줍줍 시간표 화면",
    alt: "요일과 시간대별로 강의가 배치된 주간 표. 모바일에서는 강의명과 장소가 표시된 주간 목록",
    size: [1200, 795],
    mobileSize: [840, 1260],
  },
} satisfies Record<string, { caption: string; alt: string; size: [number, number]; mobileSize: [number, number] }>;

type Preview = keyof typeof previews;

const features = [
  {
    id: "seat-alert",
    label: "빈자리 알림",
    title: "마감된 강의는\n알림을 걸어 두세요.",
    description: "빈자리가 확인되면 알림함에 남겨드려요. 웹 푸시, 이메일, Discord 중 편한 채널을 연결하면 알림을 받아볼 수 있어요.",
    preview: "notifications",
    icon: Bell,
  },
  {
    id: "timetable-preview",
    label: "내 시간표",
    title: "듣고 싶은 강의로\n시간표를 미리 짜 보세요.",
    description: "강의를 담으면서 겹치는 시간과 총 학점을 확인할 수 있어요. 시간표를 여러 개 만들어 비교하고, 이미지로 저장할 수도 있어요.",
    preview: "timetable",
    icon: CalendarDays,
  },
] satisfies { id: string; label: string; title: string; description: string; preview: Preview; icon: typeof Bell }[];

function ServicePreview({ preview, priority = false }: { preview: Preview; priority?: boolean }) {
  const { alt, caption, size: [width, height], mobileSize: [mobileWidth, mobileHeight] } = previews[preview];

  return (
    <figure className={styles.preview}>
      <div className={styles.previewFrame}>
        <picture className={styles.previewImage}>
          <source
            media="(max-width: 639px)"
            srcSet={`/landing/${preview}-mobile.webp`}
            width={mobileWidth}
            height={mobileHeight}
          />
          <Image
            src={`/landing/${preview}.webp`}
            alt={alt}
            width={width}
            height={height}
            unoptimized
            loading={priority ? "eager" : "lazy"}
            fetchPriority={priority ? "high" : undefined}
          />
        </picture>
      </div>
      <figcaption className="mt-2.5 text-center text-xs text-muted-foreground">{caption} · 예시 데이터</figcaption>
    </figure>
  );
}

export function HomeLanding() {
  return (
    <LandingMotion>
      <section className={styles.hero} aria-labelledby="landing-title">
        <div className={styles.wrap}>
          <div className={styles.heroCopy}>
            <p className="text-sm font-medium text-primary">전북대 수강신청 도우미</p>
            <h1 id="landing-title" className={styles.title}>
              기다리던 빈자리,<br />
              생기면 알려드려요.
            </h1>
            <p className={styles.description}>
              듣고 싶은 강의를 찾고, 마감된 강의는 빈자리 알림을 받아보세요.
              수강신청 전에 내 시간표도 미리 짤 수 있어요.
            </p>
            <div className="mt-7 flex flex-wrap justify-center gap-2.5 sm:mt-8">
              <Button asChild size="lg" className="h-11 rounded-lg">
                <Link href="/login">지금 시작하기 <ArrowRight aria-hidden="true" /></Link>
              </Button>
              <Button asChild size="lg" variant="outline" className="h-11 rounded-lg bg-card">
                <Link href="/search">강의 먼저 둘러보기</Link>
              </Button>
            </div>
          </div>

          <div id="course-search-preview" className={styles.heroPreview}>
            <ServicePreview preview="search" priority />
          </div>

          <nav aria-label="랜딩 기능 소개" className={styles.featureNav}>
            <a href="#course-search-preview"><Search aria-hidden="true" /> 강의 검색</a>
            <a href="#seat-alert"><Bell aria-hidden="true" /> 빈자리 알림</a>
            <a href="#timetable-preview"><CalendarDays aria-hidden="true" /> 내 시간표</a>
          </nav>
        </div>
      </section>

      {features.map(({ id, label, title, description, preview, icon: Icon }) => (
        <section
          key={id}
          id={id}
          className={preview === "timetable" ? `${styles.featureSection} ${styles.timetableSection}` : styles.featureSection}
          aria-labelledby={`${id}-title`}
          data-landing-scene
        >
          <div className={`${styles.wrap} ${styles.featureLayout}`}>
            <div className={styles.featureCopy}>
              <p className="flex items-center gap-2 text-sm font-medium text-primary"><Icon aria-hidden="true" className="size-4" />{label}</p>
              <h2 id={`${id}-title`} className={styles.featureTitle}>{title}</h2>
              <p className={styles.description}>{description}</p>
            </div>
            <ServicePreview preview={preview} />
          </div>
        </section>
      ))}

      <section className={styles.closing} aria-labelledby="landing-cta-title" data-landing-scene>
        <div className={`${styles.wrap} ${styles.featureCopy}`}>
          <h2 id="landing-cta-title" className={styles.featureTitle}>듣고 싶은 강의부터 찾아보세요.</h2>
          <Button asChild size="lg" className="mt-5 h-11 rounded-lg sm:mt-6">
            <Link href="/search">강의 검색하기 <ArrowRight aria-hidden="true" /></Link>
          </Button>
        </div>
      </section>
    </LandingMotion>
  );
}
