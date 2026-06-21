"use client";

import * as React from "react";
import { Check, ChevronDown, X } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Badge } from "@/shared/ui/badge";

interface MultiSelectFilterProps {
  options: { label: string; value: string }[];
  selected: string[];
  onChange: (selected: string[]) => void;
  placeholder?: string;
  className?: string;
  idBase?: string;
}

export function MultiSelectFilter({
  options,
  selected,
  onChange,
  placeholder = "선택",
  className,
  idBase,
}: MultiSelectFilterProps) {
  const safeBase = (idBase ?? placeholder ?? "multi-select")
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
  const triggerId = `${safeBase}-trigger`;
  const contentId = `${safeBase}-content`;

  const toggleOption = (value: string) => {
    const newSelected = selected.includes(value)
      ? selected.filter((s) => s !== value)
      : [...selected, value];
    onChange(newSelected);
  };

  const selectedLabels = options
    .filter((o) => selected.includes(o.value))
    .map((o) => o.label);

  return (
    <div className={cn("grid gap-2", className)}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            id={triggerId}
            aria-controls={contentId}
            variant="outline"
            className="h-10 w-full justify-between rounded-xl bg-muted/30 px-3 text-left font-normal hover:bg-muted/50"
          >
            <span className="truncate text-sm">
              {selectedLabels.length > 0
                ? `${selectedLabels[0]}${selectedLabels.length > 1 ? ` 외 ${selectedLabels.length - 1}개` : ""}`
                : placeholder}
            </span>
            <ChevronDown className="h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent id={contentId} aria-labelledby={triggerId} className="w-56 max-h-[300px] overflow-y-auto" align="start">
          {options.map((option) => (
            <DropdownMenuItem
              key={option.value}
              role="menuitemcheckbox"
              aria-checked={selected.includes(option.value)}
              onSelect={(event) => event.preventDefault()}
              onClick={() => toggleOption(option.value)}
              className={cn(
                "flex items-center gap-2 rounded-sm px-2 py-2 text-sm outline-hidden select-none",
                selected.includes(option.value)
                  ? "bg-primary/5 text-foreground"
                  : "text-foreground",
              )}
            >
              <span
                aria-hidden="true"
                className={cn(
                  "flex h-4 w-4 shrink-0 items-center justify-center rounded-[3px] border transition-colors",
                  selected.includes(option.value)
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-input bg-background",
                )}
              >
                {selected.includes(option.value) && <Check className="h-3 w-3" />}
              </span>
              <span className="flex-1 text-left">{option.label}</span>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {selected.map((val) => {
            const option = options.find((o) => o.value === val);
            return (
              <Badge
                key={val}
                variant="secondary"
                className="flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-medium"
              >
                {option?.label || val}
                <button
                  type="button"
                  aria-label={`${option?.label || val} 선택 해제`}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    toggleOption(val);
                  }}
                  onPointerDown={(event) => {
                    event.stopPropagation();
                  }}
                  data-testid={`remove-option-${val}`}
                  className="inline-flex h-4 w-4 items-center justify-center rounded-full opacity-50 transition-opacity hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            );
          })}
        </div>
      )}
    </div>
  );
}
