"use client";

import { usePathname } from "next/navigation";
import { useLayoutEffect, useRef, type ReactNode } from "react";

export function PageEntrance({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const previousPathname = useRef(pathname);
  const content = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    if (previousPathname.current === pathname || !content.current) {
      return;
    }

    previousPathname.current = pathname;
    const page = content.current;
    page.style.animationName = "none";
    window.getComputedStyle(page).getPropertyValue("animation-name");
    page.style.removeProperty("animation-name");
  }, [pathname]);

  return <div ref={content} className="page-entrance">{children}</div>;
}
