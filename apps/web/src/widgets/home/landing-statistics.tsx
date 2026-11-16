import { useEffect, useRef, useState } from "react";

const statistics = [
  { label: "누적 사용자", minimum: 700, unit: "명" },
  { label: "보낸 빈자리 알림", minimum: 7000, unit: "개" },
];
const countFormatter = new Intl.NumberFormat("ko-KR");
const visibleThreshold = 0.75;
const countUpDurationMs = 1400;

export function LandingStatistics() {
  const cardRef = useRef<HTMLDListElement>(null);
  const [progress, setProgress] = useState(1);

  useEffect(() => {
    const card = cardRef.current;
    if (!card || typeof IntersectionObserver === "undefined") return;

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (reducedMotion.matches) return;

    const counts = card.querySelectorAll("[data-landing-stat-count]");
    const visibleCounts = new Set<Element>();
    let hasEntered = false;
    let frameId: number | undefined;
    let startedAt: number | undefined;

    function advanceCount(timestamp: number) {
      if (reducedMotion.matches) {
        setProgress(1);
        return;
      }

      startedAt ??= timestamp;
      const elapsed = Math.min((timestamp - startedAt) / countUpDurationMs, 1);
      setProgress(1 - (1 - elapsed) ** 3);

      if (elapsed < 1) frameId = window.requestAnimationFrame(advanceCount);
    }

    const observer = new IntersectionObserver((entries) => {
      for (const { target, isIntersecting, intersectionRatio } of entries) {
        if (isIntersecting && intersectionRatio >= visibleThreshold) {
          visibleCounts.add(target);
        } else {
          visibleCounts.delete(target);
        }
      }
      if (hasEntered || visibleCounts.size !== counts.length) return;

      hasEntered = true;
      observer.disconnect();
      if (reducedMotion.matches) return;

      setProgress(0);
      frameId = window.requestAnimationFrame(advanceCount);
    }, { threshold: visibleThreshold });

    counts.forEach((count) => observer.observe(count));

    return () => {
      observer.disconnect();
      if (frameId !== undefined) window.cancelAnimationFrame(frameId);
    };
  }, []);

  return (
    <section aria-label="서비스 이용 통계" className="mx-auto w-full max-w-[1200px] px-6 pb-12 md:pb-16">
      <dl ref={cardRef} className="grid grid-cols-2 divide-x rounded-2xl border bg-card">
        {statistics.map(({ label, minimum, unit }) => (
          <div key={label} className="px-4 py-5 sm:px-7 sm:py-6">
            <dt className="break-keep text-sm text-muted-foreground">{label}</dt>
            <dd className="mt-1.5 text-2xl font-bold tabular-nums text-primary sm:text-4xl">
              <span className="sr-only">{countFormatter.format(minimum)}+{unit}</span>
              <span aria-hidden="true" data-landing-stat-count>
                {countFormatter.format(Math.floor(minimum * progress))}+<span className="ml-1 text-sm font-medium">{unit}</span>
              </span>
            </dd>
          </div>
        ))}
      </dl>
      <p className="mt-2.5 text-right text-xs text-muted-foreground">
        <time dateTime="2026-11-16">2026년 11월 16일</time> 기준
      </p>
    </section>
  );
}
