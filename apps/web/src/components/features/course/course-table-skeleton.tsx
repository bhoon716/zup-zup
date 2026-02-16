export function CourseTableSkeleton() {
  return (
    <div className="space-y-2.5">
      {[...Array(6)].map((_, index) => (
        <div
          key={index}
          className="overflow-hidden rounded-xl border border-border bg-white shadow-sm animate-pulse"
        >
          <div className="flex items-stretch">
            <div className="w-1.5 shrink-0 bg-muted" />
            <div className="flex flex-1 flex-col gap-3 p-4 md:flex-row md:items-center">
              <div className="flex-1 space-y-2">
                <div className="flex items-center gap-2">
                  <div className="h-5 w-14 rounded bg-muted" />
                  <div className="h-5 w-12 rounded bg-muted" />
                  <div className="h-4 w-16 rounded bg-muted" />
                </div>
                <div className="h-5 w-52 rounded bg-muted" />
                <div className="h-4 w-64 rounded bg-muted" />
              </div>
              <div className="min-w-[170px] space-y-2 border-t border-border pt-2 md:border-l md:border-t-0 md:pl-4 md:pt-0">
                <div className="h-4 w-24 rounded bg-muted" />
                <div className="h-1.5 w-full rounded bg-muted" />
                <div className="flex gap-1.5">
                  <div className="h-8 w-8 rounded-lg bg-muted" />
                  <div className="h-8 w-8 rounded-lg bg-muted" />
                  <div className="h-8 w-8 rounded-lg bg-muted" />
                </div>
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
