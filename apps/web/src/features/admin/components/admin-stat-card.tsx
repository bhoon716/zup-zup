import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface AdminStatCardProps {
  title: string;
  icon: LucideIcon;
  children: ReactNode;
}

export function AdminStatCard({ title, icon: Icon, children }: AdminStatCardProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <h2 className="mb-5 flex items-center gap-3 text-lg font-bold tracking-tight text-slate-900">
        <span className="rounded-xl bg-primary/10 p-2.5 text-primary">
          <Icon aria-hidden="true" className="size-5" />
        </span>
        {title}
      </h2>
      <dl className="grid gap-4 border-t border-slate-100 pt-4 min-[360px]:grid-cols-2">
        {children}
      </dl>
    </section>
  );
}
