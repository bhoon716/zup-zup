import { Fragment, type ReactNode } from "react";
import { ArrowRight, Search } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { LandingCourseTools } from "./landing-course-tools";
import { LandingWalkthrough } from "./landing-walkthrough";

import styles from "./landing-motion.module.css";

const introduction = ["수강신청 빈자리,", "이제 알림으로 잡으세요."];

const statistics = [
  { label: "누적 가입자", minimum: 680, unit: "명" },
  { label: "빈자리 알림 발송", minimum: 7000, unit: "건" },
];
const countFormatter = new Intl.NumberFormat("ko-KR");

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
    <main className="flex flex-col bg-background">
      <section className={`${styles.hero} mx-auto w-full max-w-[1200px] px-5 pt-14 pb-12 text-center sm:px-6 sm:pt-20 sm:pb-16 lg:pt-28 lg:pb-24`}>
        <h1 className={styles.heroTitle} aria-label={introduction.join(" ")}>
          {introduction.map((line) => (
            <span key={line} className={styles.heroLine}>
              {line.split(" ").map((word, index) => (
                <Fragment key={word}>
                  {index > 0 && " "}
                  <span className={styles.heroWord}><span>{word}</span></span>
                </Fragment>
              ))}
            </span>
          ))}
        </h1>
        <p className="mx-auto mt-5 max-w-[35rem] break-keep text-sm leading-relaxed text-muted-foreground sm:mt-7 sm:text-lg">
          듣고 싶은 분반이 마감됐다면 알림을 등록해 두세요. 수강신청 화면을 계속 새로고침하지 않아도, 빈자리가 나면 웹 푸시로 알려드려요.
        </p>
        <div className={`${styles.heroActions} mt-6 flex flex-wrap justify-center gap-2.5 sm:mt-8`}>
          <Button asChild className="h-11 rounded-full text-sm font-semibold has-[>svg]:px-5 sm:h-12 sm:has-[>svg]:px-6">
            <Link href="/login">
              지금 시작하기
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </Button>
          <Button asChild variant="outline" className="h-11 rounded-full border-primary/20 text-sm font-semibold text-primary hover:bg-primary/5 has-[>svg]:px-5 sm:h-12 sm:has-[>svg]:px-6">
            <Link href="/search">
              <Search className="size-4" aria-hidden="true" />
              강의 먼저 둘러보기
            </Link>
          </Button>
        </div>
        <div className="mt-6 flex justify-center empty:hidden sm:mt-8">{countdown}</div>
      </section>

      <LandingWalkthrough />

      <LandingCourseTools />

      <section className="pb-12 md:pb-16">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="grid gap-5 border-t pt-10 md:gap-7 md:pt-14">
            <div>
              <h2 className="text-2xl font-bold tracking-tight md:text-4xl">
                생생한 사용자 후기
              </h2>
              <p className="mt-2 text-sm text-muted-foreground md:text-base">
                줍줍 서비스를 이용한 대학생들의 실제 이용 후기입니다.
              </p>
            </div>
            <div className={styles.reviewViewport} tabIndex={0} role="region" aria-label="사용자 후기">
              <div className={styles.reviewTrack}>
                <ReviewList />
                <ReviewList repeated />
              </div>
            </div>
          </div>
        </div>
      </section>
      <section aria-label="서비스 이용 통계" className="mx-auto w-full max-w-[1200px] px-6 pb-12 md:pb-20">
        <dl className="grid grid-cols-2 divide-x rounded-3xl border bg-card">
          {statistics.map(({ label, minimum, unit }) => (
            <div key={label} className="px-4 py-5 sm:px-8 sm:py-8">
              <dt className="break-keep text-sm text-muted-foreground">{label}</dt>
              <dd className="mt-1.5 text-2xl font-bold tabular-nums text-primary sm:text-4xl">
                {countFormatter.format(minimum)}+<span className="ml-1 text-sm font-medium">{unit}</span>
              </dd>
            </div>
          ))}
        </dl>
        <p className="mt-2.5 text-right text-xs text-muted-foreground">
          <time dateTime="2026-11-15">2026년 11월 15일</time> 기준
        </p>
      </section>
    </main>
  );
}
