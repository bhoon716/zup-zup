import { Search } from "lucide-react";
import Image from "next/image";

import { BRAND_ICON_32_PATH } from "@/shared/seo/branding";

import styles from "./landing-hero-preview.module.css";
import { HeroSearchPreview } from "./landing-walkthrough-previews";

type SeatNotification = {
  readonly courseName: string;
  readonly section: string;
  readonly seats: number;
};

const seatNotifications: readonly [SeatNotification, SeatNotification, SeatNotification] = [
  { courseName: "자료구조", section: "01", seats: 1 },
  { courseName: "운영체제", section: "02", seats: 2 },
  { courseName: "데이터베이스", section: "03", seats: 1 },
];

export function LandingHeroPreview() {
  return (
    <div
      aria-hidden="true"
      inert
      className={`${styles.panel} grid items-center gap-3 rounded-[1.25rem] border p-3 sm:gap-4 sm:p-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,20rem)] lg:gap-6 lg:p-6`}
    >
      <div className="min-w-0 overflow-hidden rounded-xl border bg-card shadow-sm shadow-primary/5">
        <div className="flex items-center gap-4 border-b px-3 py-2.5 text-sm sm:px-4">
          <span className="flex items-center gap-1.5 font-bold text-primary">
            <Image src={BRAND_ICON_32_PATH} alt="" width={22} height={22} />
            줍줍
          </span>
          <span className="flex items-center gap-1.5 font-medium text-muted-foreground">
            <Search className="size-3.5" /> 강의 검색
          </span>
        </div>
        <div className="bg-background/75 p-3 sm:p-4">
          <HeroSearchPreview />
        </div>
      </div>
      <div className={styles.notificationViewport} data-testid="landing-hero-notifications">
        <div className={styles.notificationTrack} data-testid="landing-hero-notification-track">
          {["first", "repeat"].map((group) => (
            <div key={group} className={styles.notificationGroup}>
              {seatNotifications.map(({ courseName, section, seats }) => (
                <div
                  key={courseName}
                  className="flex items-center gap-2.5 rounded-xl border bg-card px-2.5 sm:gap-3 sm:px-3.5"
                  data-testid="landing-hero-notification"
                >
                  <span className="grid size-9 shrink-0 place-items-center rounded-[0.625rem] border border-[#572970]/10 bg-white">
                    <Image src={BRAND_ICON_32_PATH} alt="" width={22} height={22} />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm leading-5 font-bold tracking-tight text-card-foreground">{courseName}</p>
                    <p className="mt-0.5 text-xs leading-4 text-muted-foreground">{section}분반 · 빈자리 알림</p>
                  </div>
                  <span className="shrink-0 rounded-full bg-primary/[0.08] px-2.5 py-1 text-xs leading-4 font-semibold whitespace-nowrap text-primary">
                    {seats}석
                  </span>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
