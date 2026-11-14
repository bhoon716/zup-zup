import { Bell, BellPlus, Search, SlidersHorizontal } from "lucide-react";

const sections = [
  { number: "01", schedule: "월·수 10:00–11:15", seats: 1 },
  { number: "02", schedule: "화·목 13:00–14:15", seats: 0 },
  { number: "03", schedule: "화·목 15:00–16:15", seats: 0 },
];

export function LandingHeroPreview() {
  return (
    <div aria-hidden="true" className="relative h-64 overflow-hidden rounded-2xl bg-primary/10 sm:h-[21rem] lg:h-[23rem]">
      <div
        className="absolute top-11 right-[-1rem] left-[12%] overflow-hidden rounded-xl border bg-card shadow-lg shadow-primary/10 sm:top-9 sm:right-[6%] sm:left-[24%]"
      >
        <div className="flex items-center gap-3 border-b px-4 py-2.5 sm:px-5">
          <span className="text-sm font-bold text-primary">줍줍</span>
          <span className="text-xs text-muted-foreground">강의 검색</span>
        </div>
        <div className="grid gap-3 bg-background p-3 sm:grid-cols-[minmax(7.5rem,1fr)_3fr] sm:gap-4 sm:p-4">
          <div className="hidden space-y-2 rounded-lg border bg-card p-3 sm:block">
            <p className="mb-3 flex items-center gap-1.5 text-xs font-semibold">
              <SlidersHorizontal className="size-3 text-primary" /> 기본 정보
            </p>
            <p className="text-xs text-muted-foreground">이수 구분</p>
            <p className="rounded-md border px-2 py-1.5 text-xs">전공필수</p>
            <p className="text-xs text-muted-foreground">학점</p>
            <p className="rounded-md border px-2 py-1.5 text-xs">3학점</p>
          </div>
          <div className="min-w-0 space-y-2">
            <p className="flex items-center gap-2 rounded-lg border bg-card px-3 py-2 text-sm">
              <Search className="size-4 text-muted-foreground" /> 자료구조
            </p>
            <ul className="space-y-2">
              {sections.map(({ number, schedule, seats }) => (
                <li
                  key={number}
                  className={`flex items-center justify-between gap-2 rounded-lg border px-3 py-2.5 ${seats ? "border-primary/25 bg-primary/5" : "bg-card"}`}
                >
                  <div>
                    <p className="text-sm font-semibold">자료구조 <span className="text-xs font-normal text-muted-foreground">{number}분반</span></p>
                    <p className="text-xs text-muted-foreground">{schedule}</p>
                  </div>
                  <span className={`hidden shrink-0 items-center gap-2 text-xs min-[360px]:flex ${seats ? "font-semibold text-primary" : "text-muted-foreground"}`}>
                    여석 {seats} <BellPlus className="size-3.5" />
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
      <div
        className="absolute -bottom-8 left-[5%] w-[11.5rem] rounded-[1.5rem] border-4 border-foreground bg-background p-2.5 pb-5 shadow-lg shadow-primary/20 sm:left-[7%] sm:w-56 sm:p-3 sm:pb-16"
      >
        <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-foreground/80" />
        <div className="rounded-xl border bg-card p-2.5 sm:p-3">
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Bell className="size-3.5 text-primary" />
            줍줍 <span className="ml-auto">방금</span>
          </p>
          <p className="mt-2 text-sm font-semibold">빈자리가 생겼어요!</p>
          <p className="mt-1 text-xs text-muted-foreground">자료구조 01분반 · 여석 1</p>
        </div>
      </div>
    </div>
  );
}
