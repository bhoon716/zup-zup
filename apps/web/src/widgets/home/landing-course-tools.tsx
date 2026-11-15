import { ArrowRight, CalendarDays, Download, Heart, Plus, Star } from "lucide-react";
import Link from "next/link";

import styles from "./landing-motion.module.css";

const courseTools = [
  {
    title: "내 시간표",
    description: "강의 검색에서 달력 아이콘으로 수업을 담고, 빈 시간에는 개인 일정을 추가해요. 시간표를 여러 개 만들어 비교하거나 이미지로 저장할 수 있어요.",
    Icon: CalendarDays,
    Preview: TimetablePreview,
    href: "/timetable",
    action: "내 시간표 열기",
  },
  {
    title: "관심 강의 찜하기",
    description: "강의 검색에서 하트를 눌러 저장해요. 내 시간표의 ‘찜 목록’에서 수업 시간과 학점을 확인한 뒤, 선택한 시간표에 담을 수 있어요.",
    Icon: Heart,
    Preview: WishlistPreview,
    href: "/search",
    action: "찜할 강의 찾아보기",
  },
  {
    title: "강의 리뷰",
    description: "검색 결과에서 강의 제목을 누르면 상세 화면에서 평균 별점과 이모지 반응을 볼 수 있어요. 로그인하면 내 별점과 이모지도 남길 수 있어요.",
    Icon: Star,
    Preview: CourseReviewPreview,
    href: "/search",
    action: "리뷰 볼 강의 찾아보기",
  },
];

const weekDays = ["월", "화", "수", "목", "금"];
const dataStructures = { name: "자료구조", color: "bg-primary/12 text-primary" };
const englishConversation = {
  name: "영어회화",
  color: "bg-sky-100 text-sky-950 dark:bg-sky-950 dark:text-sky-100",
};
const clubMeeting = {
  name: "동아리",
  color: "bg-emerald-100 text-emerald-950 dark:bg-emerald-950 dark:text-emerald-100",
};
const timetableRows = [
  { hour: "10시", slots: [dataStructures, null, dataStructures, null, null] },
  { hour: "12시", slots: [null, englishConversation, null, englishConversation, null] },
  { hour: "14시", slots: [null, null, null, null, clubMeeting] },
];

function TimetablePreview() {
  return (
    <>
      <div className="flex items-center justify-between gap-2 text-xs font-semibold">
        <span>시간표 1안</span>
        <Download className="size-3.5 text-muted-foreground" />
      </div>
      <div className="grid flex-1 grid-cols-[auto_repeat(5,minmax(0,1fr))] grid-rows-[1.125rem_repeat(3,minmax(0,1fr))] overflow-hidden rounded-lg border bg-card text-center text-[10px] leading-none">
        <span className="border-r border-b bg-muted/50" />
        {weekDays.map((day) => (
          <span key={day} className="grid place-items-center border-r border-b font-medium text-muted-foreground last:border-r-0">
            {day}
          </span>
        ))}
        {timetableRows.map(({ hour, slots }) => (
          <div key={hour} className="contents [&:last-of-type>span]:border-b-0">
            <span className="grid place-items-center border-r border-b bg-muted/50 px-0.5 tabular-nums text-muted-foreground">
              {hour}
            </span>
            {slots.map((slot, index) => (
              <span key={weekDays[index]} className="grid min-w-0 border-r border-b p-0.5 last:border-r-0">
                {slot && (
                  <span className={`grid place-items-center rounded-sm px-0.5 font-semibold break-all ${slot.color}`}>
                    {slot.name}
                  </span>
                )}
              </span>
            ))}
          </div>
        ))}
      </div>
    </>
  );
}

const wishedCourses = [
  { name: "자료구조", schedule: "월·수 10:00–11:15", credits: 3 },
  { name: "영어회화", schedule: "화·목 12:00–13:15", credits: 3 },
];

function WishlistPreview() {
  return (
    <>
      <div className="flex items-center justify-between gap-2 text-xs">
        <span className="flex items-center gap-1.5 font-semibold">
          <Heart className="size-3.5 text-primary" />
          찜한 강의
        </span>
        <span className="text-muted-foreground">{wishedCourses.length}개</span>
      </div>
      {wishedCourses.map(({ name, schedule, credits }) => (
        <div key={name} className="flex flex-1 items-center justify-between gap-1.5 rounded-lg border bg-card px-2">
          <div className="min-w-0">
            <p className="text-xs font-semibold">{name}</p>
            <p className="text-[10px] font-medium tabular-nums text-muted-foreground">{schedule}</p>
          </div>
          <div className="flex shrink-0 items-center gap-1.5 text-[10px] leading-none font-medium text-muted-foreground">
            <span className="rounded bg-muted px-1.5 py-1">{credits}학점</span>
            <Plus className="size-3.5 text-primary" />
          </div>
        </div>
      ))}
    </>
  );
}

const rating = 4.5;
const reviewCount = 8;
const emojiReactions = [
  { emoji: "👍", count: 6 },
  { emoji: "💡", count: 4 },
  { emoji: "📚", count: 3 },
];

function CourseReviewPreview() {
  return (
    <>
      <div className="flex items-center justify-between gap-2 text-xs font-semibold">
        <span>자료구조</span>
        <span className="font-medium text-muted-foreground">리뷰 예시</span>
      </div>
      <div className="flex flex-1 flex-col justify-center gap-1.5 rounded-lg border bg-card px-2.5">
        <p className="text-[11px] font-medium text-muted-foreground">평균 별점</p>
        <div className="flex flex-wrap items-center gap-x-1.5 gap-y-0.5">
          <span className="text-lg leading-none font-bold tabular-nums text-primary">{rating}</span>
          <div className="flex items-center gap-0.5">
            {[1, 2, 3, 4, 5].map((star) => (
              <Star
                key={star}
                className={star <= Math.round(rating) ? "size-3 fill-amber-500 text-amber-500" : "size-3 text-muted-foreground"}
              />
            ))}
          </div>
          <span className="ml-auto text-[10px] whitespace-nowrap tabular-nums text-muted-foreground">{reviewCount}개</span>
        </div>
      </div>
      <div className="flex gap-1.5">
        {emojiReactions.map(({ emoji, count }) => (
          <span key={emoji} className="inline-flex items-center gap-1 rounded-full border bg-card px-2 py-1 text-xs leading-none">
            {emoji}
            <span className="text-[10px] font-medium tabular-nums text-muted-foreground">{count}</span>
          </span>
        ))}
      </div>
    </>
  );
}

export function LandingCourseTools() {
  return (
    <section
      aria-labelledby="landing-course-tools-title"
      className="mx-auto w-full max-w-[1200px] px-6 py-12 md:py-20"
    >
      <h2 id="landing-course-tools-title" className="text-2xl font-bold tracking-tight md:text-4xl">
        시간표부터 강의 리뷰까지
      </h2>
      <ul className="mt-6 grid gap-3 md:mt-8 md:grid-cols-3 md:gap-4" role="list">
        {courseTools.map(({ title, description, Icon, Preview, href, action }) => (
          <li key={title} className={`${styles.toolCard} flex min-w-0 flex-col gap-4 rounded-3xl border bg-card p-4 text-card-foreground sm:p-5`}>
            <div className="flex items-center gap-2.5">
              <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
                <Icon className="size-[18px]" aria-hidden="true" />
              </span>
              <h3 className="text-lg font-bold tracking-tight">{title}</h3>
            </div>
            <div aria-hidden="true" className={`${styles.toolIllustration} flex h-39 flex-col gap-2 rounded-2xl border border-primary/8 bg-primary/5 p-3`}>
              <Preview />
            </div>
            <p className="break-keep text-sm leading-relaxed text-muted-foreground">{description}</p>
            <Link
              href={href}
              className="-mx-1.5 mt-auto inline-flex min-h-8 w-fit items-center gap-1.5 rounded-md px-1.5 text-sm font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-ring"
            >
              {action}
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
