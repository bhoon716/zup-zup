import { Bell, BellPlus, Check, Search } from "lucide-react";

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
        <span className="whitespace-nowrap">{sampleCourse.name}</span>
      </div>
      <div className="flex flex-wrap gap-1.5">
        <Badge variant="outline" className="bg-card">전공필수</Badge>
        <Badge variant="outline" className="bg-card">{sampleCourse.credits}</Badge>
      </div>
      <ul className="grid gap-2">
        <li className="flex items-center justify-between gap-2 rounded-xl border border-primary/25 bg-primary/[0.06] px-3 py-2.5">
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

function RegistrationPreview() {
  return (
    <div className="grid gap-3">
      <div className="rounded-xl border bg-card p-4">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-sm font-bold">{sampleCourse.name} <span className="text-xs text-primary">{sampleCourse.section}</span></p>
            <p className="mt-1 text-xs text-muted-foreground">{sampleCourse.credits} · {sampleCourse.meetingTime}</p>
          </div>
          <Badge variant="outline" className="shrink-0 text-muted-foreground">여석 0</Badge>
        </div>
        <div className={styles.registrationConfirmation}>
          <Check className="size-4" /> 등록 완료
        </div>
      </div>
      <p className="px-1 text-xs font-semibold text-muted-foreground">
        빈자리가 생기면 웹 푸시를 보내드려요.
      </p>
    </div>
  );
}

function NotificationPreview() {
  return (
    <div className="grid gap-3">
      <p className="flex items-center gap-2 px-1 text-xs font-bold text-primary">
        <Check className="size-4" /> {sampleCourse.name} {sampleCourse.section} 알림 구독 중
      </p>
      <div className="rounded-xl border border-primary/20 bg-card p-4">
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

export function LandingWalkthrough() {
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
      <ol className={styles.stage} data-testid="landing-walkthrough-stage" role="list">
        {chapters.map(({ id, title, description, Icon, Preview }, index) => (
          <li key={id} className={styles.chapter}>
            <div>
              <div className={styles.stepMark} aria-hidden="true">
                <Icon className="size-[18px]" />
              </div>
              <h3 className="mt-3 text-xl font-bold tracking-tight md:text-2xl">{title}</h3>
              <p className="mt-2 max-w-sm break-keep text-sm leading-relaxed text-muted-foreground md:text-base">{description}</p>
            </div>

            <div className={styles.frame} data-testid="landing-walkthrough-preview" aria-hidden="true">
              <div className={styles.frameBar}>
                <span>줍줍 미리보기</span>
                <span className={styles.example}>화면 예시</span>
              </div>
              <div className={styles.scene}><Preview /></div>
              <div className={styles.progress}>
                {chapters.map(({ id: stepId }, step) => <span key={stepId} data-complete={step <= index || undefined} />)}
              </div>
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
