import { ChevronDown } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";
import type { ReactNode } from "react";

interface FilterSectionProps {
  idBase: string;
  title: string;
  icon?: ReactNode;
  children: ReactNode;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
}

export function FilterSection({
  idBase,
  title,
  icon,
  children,
  open,
  onOpenChange,
  className,
}: FilterSectionProps) {
  const triggerId = `${idBase}-trigger`;
  const contentId = `${idBase}-content`;

  return (
    <div
      className={cn(
        "rounded-2xl border border-border/40 bg-white/50 shadow-sm backdrop-blur-sm transition-all hover:bg-white/80",
        className,
      )}
    >
      <Button
        id={triggerId}
        type="button"
        variant="ghost"
        aria-controls={contentId}
        aria-expanded={open}
        onClick={() => onOpenChange?.(!open)}
        className="flex h-auto w-full items-center justify-between px-4 py-3 hover:bg-transparent"
      >
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
      </Button>
      <div
        id={contentId}
        hidden={!open}
        className="border-t border-border/70 px-4 pb-4 pt-3"
      >
        {children}
      </div>
    </div>
  );
}
