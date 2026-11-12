import Image from "next/image";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { LandingMotion } from "./landing-motion";
import styles from "./home-landing.module.css";

const scenes = [
  {
    id: "course-search-preview",
    label: "강의 검색",
    title: "듣고 싶은 강의를 찾고.",
    description: "강의명, 교수명, 시간대로 검색하고 남은 자리를 확인하세요.",
    image: "search",
    alt: "검색 필터와 강의별 수강 인원 및 빈자리가 표시된 강의 목록",
    size: [1240, 732],
    mobileSize: [840, 1394],
  },
  {
    id: "seat-alert",
    label: "빈자리 알림",
    title: "빈자리가 나면 알림 받고.",
    description: "마감된 강의에 알림을 걸어 두세요. 빈자리가 확인되면 알림함과 연결한 채널로 알려드려요.",
    image: "notifications",
    alt: "과목별 빈자리 수와 확인 시각이 최근 순으로 표시된 알림함",
    size: [1050, 620],
    mobileSize: [840, 1478],
  },
  {
    id: "timetable-preview",
    label: "내 시간표",
    title: "시간표를 미리 짜 보세요.",
    description: "고른 강의를 담고 겹치는 시간과 총 학점을 확인하세요.",
    image: "timetable",
    alt: "요일과 시간대별로 강의명과 장소가 표시된 주간 시간표",
    size: [1200, 795],
    mobileSize: [840, 1260],
  },
] satisfies {
  id: string;
  label: string;
  title: string;
  description: string;
  image: string;
  alt: string;
  size: [number, number];
  mobileSize: [number, number];
}[];

function StoryScene({ scene, first }: { scene: (typeof scenes)[number]; first: boolean }) {
  const { id, title, description, image, alt, size: [width, height], mobileSize: [mobileWidth, mobileHeight] } = scene;

  return (
    <section id={id} className={styles.scene} aria-labelledby={`${id}-title`} data-story-scene>
      <div className={styles.sceneCopy}>
        <h2 id={`${id}-title`} className={styles.featureTitle}>{title}</h2>
        <p className={styles.description}>{description}</p>
      </div>
      <figure className={styles.preview}>
        <picture className={styles.previewImage}>
          <source
            media="(max-width: 639px)"
            srcSet={`/landing/${image}-mobile.webp`}
            width={mobileWidth}
            height={mobileHeight}
          />
          <Image
            src={`/landing/${image}.webp`}
            alt={alt}
            width={width}
            height={height}
            unoptimized
            loading="eager"
            fetchPriority={first ? "high" : "low"}
          />
        </picture>
        <figcaption className="sr-only">실제 줍줍 화면 · 예시 데이터</figcaption>
      </figure>
    </section>
  );
}

export function HomeLanding() {
  return (
    <LandingMotion>
      <section className={styles.hero} aria-labelledby="landing-title">
        <div className={`${styles.wrap} ${styles.heroCopy}`}>
          <h1 id="landing-title" className={styles.title}>
            기다리던 빈자리,<br /> 생기면 알려드려요.
          </h1>
          <div>
            <p className={styles.description}>
              전북대 강의를 찾고, 마감된 강의는 빈자리 알림을 받으세요.
              내 시간표도 미리 만들 수 있어요.
            </p>
            <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2">
              <Button asChild size="lg" className="h-11 rounded-full">
                <Link href="/login">지금 시작하기 <ArrowRight aria-hidden="true" /></Link>
              </Button>
              <Button asChild variant="link" className="h-11 px-0">
                <Link href="/search">강의 먼저 둘러보기</Link>
              </Button>
            </div>
          </div>
        </div>
      </section>

      <div className={`${styles.wrap} ${styles.story}`} data-landing-story>
        <div className={styles.stage} data-story-stage>
          <div className={styles.scenes}>
            {scenes.map((scene, index) => <StoryScene key={scene.id} scene={scene} first={index === 0} />)}
          </div>
          <nav aria-label="랜딩 기능 소개" className={styles.featureNav}>
            {scenes.map(({ id, label }) => (
              <a key={id} href={`#${id}`} data-story-link>
                {label}
              </a>
            ))}
          </nav>
        </div>
      </div>

      <section className={styles.closing} aria-labelledby="landing-cta-title">
        <div className={styles.wrap}>
          <h2 id="landing-cta-title" className={styles.featureTitle}>듣고 싶은 강의부터 찾아보세요.</h2>
          <Button asChild size="lg" className="mt-5 h-11 rounded-lg sm:mt-6">
            <Link href="/search">강의 검색하기 <ArrowRight aria-hidden="true" /></Link>
          </Button>
        </div>
      </section>
    </LandingMotion>
  );
}
