import { ChevronDown } from "lucide-react";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/shared/ui/collapsible";
import { cn } from "@/shared/lib/utils";
import type { ReactNode } from "react";

interface FilterSectionProps {
  title: string;
  icon?: ReactNode;
  children: ReactNode;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
}

export function FilterSection({
  title,
  icon,
  children,
  open,
  onOpenChange,
  className,
}: FilterSectionProps) {
  return (
    <Collapsible open={open} onOpenChange={onOpenChange} className={className}>
      <div className="rounded-2xl border border-border/40 bg-white/50 shadow-sm backdrop-blur-sm transition-all hover:bg-white/80">
        <CollapsibleTrigger className="flex w-full items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            {icon}
            <span className="text-sm font-bold text-foreground/80">{title}</span>
          </div>
          <div
            className={cn(
              "rounded-lg p-1 transition-colors hover:bg-muted",
              open && "rotate-180",
            )}
          >
            <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform duration-300" />
          </div>
        </CollapsibleTrigger>
        <CollapsibleContent className="border-t border-border/70 px-4 pb-4 pt-3 data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up">
          {children}
        </CollapsibleContent>
      </div>
    </Collapsible>
  );
}
