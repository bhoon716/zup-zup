import { CalendarDays, ClipboardList } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import type { ScheduleResponse } from "@/shared/types/api";
import { dashboardListStyles as styles } from "./dashboard-list-styles";

export function DashboardDDayBlock({ upcomingSchedules }: { upcomingSchedules: ScheduleResponse[] }) {
  return (
    <div className={styles.card}>
      <h3 className={styles.header}>
        <span className={styles.icon}>
          <CalendarDays aria-hidden="true" className="size-5" />
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
            const isActive = event.dDay === "D-Day" || event.dDay === "진행 중";
            const start = [event.startDate, event.startTime].filter(Boolean).join(" ");
            const end = [event.endDate, event.endTime].filter(Boolean).join(" ");
            return (
              <li
                key={event.id}
                className={cn(
                  "relative flex min-w-0 items-center gap-2.5 rounded-2xl border p-3 sm:gap-4 sm:p-4",
                  isActive
                    ? "border-primary/20 bg-white shadow-sm dark:bg-gray-800"
                    : "border-transparent bg-gray-50/50 dark:bg-gray-900/50",
                )}
              >
                {isActive && <span aria-hidden="true" className="absolute inset-y-3 left-0 w-1 rounded-r-full bg-primary" />}
                <span className={cn(
                  "grid size-9 shrink-0 place-items-center rounded-xl sm:size-12 sm:rounded-2xl",
                  isActive
                    ? "bg-red-50 text-red-600 dark:bg-red-950/30 dark:text-red-400"
                    : "bg-indigo-50 text-indigo-600 dark:bg-indigo-950/30 dark:text-indigo-400",
                )}>
                  <ClipboardList aria-hidden="true" className="size-5 sm:size-6" />
                </span>
                <div className="min-w-0 flex-1">
                  <h4 className={cn(styles.title, "wrap-anywhere break-keep")}>{event.scheduleType}</h4>
                  <p className="mt-1 text-xs text-muted-foreground">{start} ~ {end}</p>
                </div>
                <span className={cn(
                  "min-w-12 shrink-0 rounded-xl px-2 py-1.5 text-center text-xs font-bold sm:min-w-14 sm:px-3 sm:text-sm",
                  isActive
                    ? "bg-primary text-primary-foreground shadow-sm shadow-primary/20"
                    : "border border-border bg-card text-muted-foreground",
                )}>
                  {event.dDay}
                </span>
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
