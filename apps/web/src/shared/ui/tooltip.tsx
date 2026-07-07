"use client";

import * as React from "react";
import * as TooltipPrimitive from "@radix-ui/react-tooltip";

import { cn } from "@/shared/lib/utils";

/**
 * 전역 툴팁 프로바이더입니다.
 * 앱 최상위(Providers)에서 한 번만 선언해야 합니다.
 */
const TooltipProvider = TooltipPrimitive.Provider;

/**
 * 툴팁의 루트 컴포넌트입니다.
 */
const Tooltip = TooltipPrimitive.Root;

/**
 * 툴팁을 활성화하는 트리거 컴포넌트입니다.
 */
const TooltipTrigger = TooltipPrimitive.Trigger;

/**
 * 툴팁의 실제 콘텐츠 내용이 들어가는 컴포넌트입니다.
 * 애니메이션 및 기본 스타일이 정의되어 있습니다.
 */
const TooltipContent = React.forwardRef<
  React.ElementRef<typeof TooltipPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof TooltipPrimitive.Content>
>(({ className, sideOffset = 4, ...props }, ref) => (
  <TooltipPrimitive.Portal>
    <TooltipPrimitive.Content
      ref={ref}
      sideOffset={sideOffset}
      className={cn(
        "z-100 overflow-hidden rounded-md bg-slate-900 px-3 py-1.5 text-[11px] text-white shadow-xl animate-in fade-in duration-150 data-[side=bottom]:slide-in-from-top-1 data-[side=left]:slide-in-from-right-1 data-[side=right]:slide-in-from-left-1 data-[side=top]:slide-in-from-bottom-1",
        className
      )}
      {...props}
    />
  </TooltipPrimitive.Portal>
));
TooltipContent.displayName = TooltipPrimitive.Content.displayName;

export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider };
