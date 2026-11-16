export const dashboardListStyles = {
  card: "h-full rounded-[2.5rem] border border-gray-50 bg-white p-4 shadow-floating sm:p-5 dark:border-gray-800 dark:bg-gray-900",
  header: "mb-3 flex items-center gap-3 text-xl font-bold text-gray-900 dark:text-gray-100",
  icon: "grid size-10 shrink-0 place-items-center rounded-2xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/30",
  row: "block rounded-2xl border border-gray-50 p-3 dark:border-gray-800/50",
  title: "min-w-0 text-sm font-bold text-gray-800 dark:text-gray-200",
  summary: "mt-2 text-xs text-slate-500 dark:text-slate-400",
  metadata: "shrink-0 text-[10px] font-medium text-gray-500 dark:text-gray-400",
  empty: "rounded-2xl border border-dashed border-border bg-muted/50 px-4 py-8 text-center text-sm text-muted-foreground",
} as const;
