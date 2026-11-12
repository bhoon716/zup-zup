"use client";

import { useEffect, useRef, type ReactNode } from "react";
import styles from "./home-landing.module.css";

export function LandingMotion({ children }: { children: ReactNode }) {
  const root = useRef<HTMLElement>(null);

  useEffect(() => {
    if (!root.current || typeof IntersectionObserver === "undefined") return;

    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.setAttribute("data-landing-visible", "");
        observer.unobserve(entry.target);
      }
    }, { rootMargin: "0px 0px -12% 0px", threshold: 0.18 });

    root.current.querySelectorAll("[data-landing-scene]").forEach((section) => observer.observe(section));
    return () => observer.disconnect();
  }, []);

  return <main ref={root} className={styles.landing}>{children}</main>;
}
