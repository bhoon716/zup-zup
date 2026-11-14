import type { ReactNode } from "react";
import { Pause, Play } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { LandingWalkthrough } from "./landing-walkthrough";

import styles from "./landing-walkthrough.module.css";

const everyTimeReviews = [
  {
    id: 1,
    content: "덕분에 계절 자리 잡았습니다 감사합니다",
    source: "에브리타임",
  },
  {
    id: 2,
    content: "너무너무 감사합니다… 알림 뜨는거 보고 바로 들어가면 자리 있더라고요!! 덕분에 시간표 예쁘게 만들었어요 감사합니다!!🥹🫶🏻",
    source: "에브리타임",
  },
  {
    id: 3,
    content: "덕분에 전공 빈자리 잡았습니다 ㅜㅜ 감사합니다!!",
    source: "에브리타임",
  },
  {
    id: 4,
    content: "수강신청 실패해서 한 과목 부족했는데 알림 덕분에 안전하게 잡았어요!! 진짜 유용하게 잘 쓰고 있습니다ㅠㅠ",
    source: "에브리타임",
  },
  {
    id: 5,
    content: "알림 속도 엄청 빨라요! 뜨자마자 바로 들어가서 겨우 성공했습니다.. 개발자님 복 받으세요!",
    source: "에브리타임",
  },
  {
    id: 6,
    content: "알림 덕분에 졸업 필수 전공 드디어 잡았어요ㅠㅠ 진짜 은인이십니다 감사합니다!!",
    source: "에브리타임",
  },
];

function ReviewList({ repeated = false }: { repeated?: boolean }) {
  return (
    <ul
      aria-hidden={repeated || undefined}
      aria-label={repeated ? undefined : "사용자 후기 목록"}
      className={styles.reviewList}
      role="list"
    >
      {everyTimeReviews.map((review) => (
        <li
          key={review.id}
          className="flex min-h-40 min-w-0 flex-col justify-between rounded-2xl border border-slate-100 bg-white p-5 shadow-xs dark:border-primary/5 dark:bg-primary/5"
        >
          <div>
            <p className="mb-3 text-xs font-bold text-slate-800 dark:text-slate-200">익명</p>
            <p className="text-[13px] font-semibold leading-relaxed text-slate-600 dark:text-slate-300">
              {review.content}
            </p>
          </div>
          <p className="mt-4 text-right text-[10px] font-bold text-slate-500 dark:text-slate-400">
            {review.source}
          </p>
        </li>
      ))}
    </ul>
  );
}

export function HomeLanding({ countdown }: { countdown: ReactNode }) {
  return (
    <main className="flex flex-col bg-[#fafafa] dark:bg-black">
      <section className="py-10 sm:py-14 md:pt-20 md:pb-24">
        <div className="mx-auto flex w-full max-w-[1200px] flex-col items-center px-6 text-center">
          <h1
            className="mb-5 text-[clamp(1.5rem,7vw,2.125rem)] leading-[1.15] font-bold tracking-tight text-foreground md:text-[2.75rem] lg:text-5xl"
          >
            전북대 수강신청 빈자리,<br />
            <span className="text-primary">이제 알림으로 잡으세요.</span>
          </h1>

          <p
            className="mx-auto mb-6 max-w-[34rem] break-keep text-base leading-relaxed text-muted-foreground md:mb-8 md:text-lg"
          >
            하루 종일 새로고침만 누르고 계신가요? 전북대 수강신청 빈자리 알림 서비스 &apos;줍줍&apos;이 빈자리가 생기면 가장 먼저 알려드립니다. 스마트한 시간표 관리까지 한 번에 해결하세요.
          </p>

          <div className="mb-6 flex w-full justify-center empty:hidden">{countdown}</div>

          <div
            className="flex w-full flex-col items-center justify-center gap-3 sm:w-auto sm:flex-row"
          >
            <Button asChild className="h-auto w-full rounded-full px-6 py-3.5 text-sm font-bold shadow-md shadow-primary/20 sm:w-auto">
              <Link href="/login">
                지금 시작하기
              </Link>
            </Button>
            <Button asChild variant="outline" className="h-auto w-full rounded-full border-primary/20 px-6 py-3.5 text-sm font-bold text-primary hover:bg-primary/5 sm:w-auto">
              <Link href="/search">
                강의 먼저 둘러보기
              </Link>
            </Button>
          </div>
        </div>
      </section>

      <LandingWalkthrough />

      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-4 flex flex-wrap items-end justify-between gap-x-5 gap-y-3">
            <div className="min-w-0">
              <h2 className="text-2xl md:text-3xl font-bold text-[#161118] dark:text-white flex items-center gap-3">
                생생한 사용자 후기
              </h2>
              <p className="mt-2 text-[#161118]/60 dark:text-slate-400 text-sm md:text-base font-medium">
                줍줍 서비스를 이용한 대학생들의 실제 이용 후기입니다.
              </p>
            </div>
            <input id="pause-landing-reviews" type="checkbox" className={styles.pauseReviews} />
            <label htmlFor="pause-landing-reviews" className={styles.reviewControl}>
              <span className="sr-only">후기 자동 이동 일시정지 또는 다시 재생</span>
              <span className={styles.pauseLabel} aria-hidden="true"><Pause className="size-3.5" />일시정지</span>
              <span className={styles.playLabel} aria-hidden="true"><Play className="size-3.5" />다시 재생</span>
            </label>
            <div className={styles.reviewViewport}>
              <div className={styles.reviewTrack}>
                <ReviewList />
                <ReviewList repeated />
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
