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
        "rounded-lg border bg-card",
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
        className="flex h-10 w-full items-center justify-between rounded-lg px-2.5 py-2 has-[>svg]:px-2.5"
      >
        <div className="flex items-center gap-2">
          {icon}
          <span className="text-sm font-semibold text-foreground">{title}</span>
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
        className="border-t px-2.5 py-3"
      >
        {children}
      </div>
    </div>
  );
}
