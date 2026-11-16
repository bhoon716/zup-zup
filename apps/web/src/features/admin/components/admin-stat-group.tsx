import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface AdminStatGroupProps {
  title: string;
  icon: LucideIcon;
  children: ReactNode;
}

export function AdminStatGroup({ title, icon: Icon, children }: AdminStatGroupProps) {
  return (
    <section className="min-w-0 px-4 py-3.5 sm:px-5 sm:py-4">
      <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-900">
        <Icon aria-hidden="true" className="size-4 text-primary" />
        {title}
      </h2>
      <dl className="grid gap-x-3 gap-y-4 min-[360px]:grid-cols-2">
        {children}
      </dl>
    </section>
  );
}
