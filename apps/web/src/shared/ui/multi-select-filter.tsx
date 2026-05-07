"use client";

import * as React from "react";
import { Check, ChevronDown, X } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Badge } from "@/shared/ui/badge";

interface MultiSelectFilterProps {
  options: { label: string; value: string }[];
  selected: string[];
  onChange: (selected: string[]) => void;
  placeholder?: string;
  className?: string;
}

export function MultiSelectFilter({
  options,
  selected,
  onChange,
  placeholder = "선택",
  className,
}: MultiSelectFilterProps) {
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
        <DropdownMenuContent className="w-56 max-h-[300px] overflow-y-auto" align="start">
          {options.map((option) => (
            <DropdownMenuCheckboxItem
              key={option.value}
              checked={selected.includes(option.value)}
              onCheckedChange={() => toggleOption(option.value)}
              onSelect={(e) => e.preventDefault()}
            >
              {option.label}
            </DropdownMenuCheckboxItem>
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
                <X
                  className="h-3 w-3 cursor-pointer opacity-50 hover:opacity-100"
                  onClick={() => toggleOption(val)}
                  data-testid={`remove-option-${val}`}
                />
              </Badge>
            );
          })}
        </div>
      )}
    </div>
  );
}
