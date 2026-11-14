import { Timer } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import type { ScheduleResponse } from "@/shared/types/api";
import { dashboardListStyles as styles } from "./dashboard-list-styles";

export function DashboardDDayBlock({ upcomingSchedules }: { upcomingSchedules: ScheduleResponse[] }) {
  return (
    <div className={styles.card}>
      <h3 className={styles.header}>
        <span className={styles.icon}>
          <Timer aria-hidden="true" className="size-5" />
        </span>
        주요 일정
      </h3>

      {upcomingSchedules.length === 0 ? (
        <p className={styles.empty}>현재 등록된 주요 일정이 없습니다.</p>
      ) : (
        <ul
          className="max-h-88 space-y-2 overflow-y-auto overscroll-contain pr-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          aria-label="주요 일정 목록"
          role="list"
          tabIndex={0}
        >
          {upcomingSchedules.map((event) => {
            const isOngoing = event.dDay === "D-Day" || event.dDay === "진행 중";
            const start = [event.startDate, event.startTime].filter(Boolean).join(" ");
            const end = [event.endDate, event.endTime].filter(Boolean).join(" ");
            return (
              <li key={event.id} className={styles.row}>
                <div className="flex items-start justify-between gap-3">
                  <h4 className={cn(styles.title, "wrap-anywhere break-keep")}>{event.scheduleType}</h4>
                  <span className={cn(styles.metadata, isOngoing && "rounded bg-primary/10 px-1.5 py-0.5 text-primary")}>
                    {event.dDay}
                  </span>
                </div>
                <p className={styles.summary}>{start} ~ {end}</p>
              </li>
            );
          })}
        </ul>
      )}

      <p className="mt-3 border-t pt-3 text-xs text-muted-foreground">
        정확한 일정은 전북대학교 오아시스 및 수강신청 시스템에서 확인해 주세요.
      </p>
    </div>
  );
}
