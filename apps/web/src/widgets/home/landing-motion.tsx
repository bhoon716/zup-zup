"use client";

import { useEffect, useRef, type ReactNode } from "react";
import styles from "./home-landing.module.css";

const storyMotion = "(prefers-reduced-motion: no-preference) and (min-height: 560px)";
const crossoverStart = 0.38;
const crossoverEnd = 1 - crossoverStart;

function clampProgress(value: number, max = 1) {
  return Math.min(max, Math.max(0, value));
}

export function LandingMotion({ children }: { children: ReactNode }) {
  const root = useRef<HTMLElement>(null);

  useEffect(() => {
    const landing = root.current;
    const story = landing?.querySelector<HTMLElement>("[data-landing-story]");
    const stage = story?.querySelector<HTMLElement>("[data-story-stage]");
    if (!landing || !story || !stage) return;

    const scenes = Array.from(stage.querySelectorAll<HTMLElement>("[data-story-scene]"));
    const links = stage.querySelectorAll<HTMLAnchorElement>("[data-story-link]");
    const lastSceneIndex = scenes.length - 1;
    if (lastSceneIndex < 1) return;

    const motion = window.matchMedia(storyMotion);
    let frame = 0;
    let alignHash = true;
    let previousProgress = -1;
    let activeIndex = -1;

    function sceneIndex(hash: string) {
      return scenes.findIndex((scene) => `#${scene.id}` === hash);
    }

    const measureStory = () => {
      const stickyTop = Number.parseFloat(getComputedStyle(stage).top);
      return {
        distance: stickyTop - story.getBoundingClientRect().top,
        range: story.offsetHeight - stage.offsetHeight,
      };
    };

    function scrollToScene(index: number, behavior: ScrollBehavior) {
      const { distance, range } = measureStory();
      const top = window.scrollY - distance + (index / lastSceneIndex) * range;
      window.scrollTo({ top, behavior });
    }

    const renderStory = () => {
      frame = 0;
      if (alignHash) {
        alignHash = false;
        const index = sceneIndex(window.location.hash);
        if (index >= 0) scrollToScene(index, "instant");
      }

      const { distance, range } = measureStory();
      const progress = range > 0 ? clampProgress((distance / range) * lastSceneIndex, lastSceneIndex) : 0;
      if (progress === previousProgress) return;

      const nextIndex = Math.round(progress);
      scenes.forEach((scene, index) => {
        const distanceFromScene = progress - index;
        const opacity = clampProgress((crossoverEnd - Math.abs(distanceFromScene)) / (crossoverEnd - crossoverStart));
        const fade = 1 - opacity;
        scene.style.setProperty("--scene-opacity", String(opacity));
        scene.style.setProperty("--scene-copy-opacity", String(Math.max(0, 2 * opacity - 1)));
        scene.style.setProperty("--scene-offset", `${-Math.sign(distanceFromScene) * fade * 18}px`);
        scene.style.setProperty("--scene-scale", String(1 - fade * 0.02));
        if (nextIndex !== activeIndex) {
          if (index === nextIndex) scene.removeAttribute("aria-hidden");
          else scene.setAttribute("aria-hidden", "true");
        }
      });

      if (nextIndex !== activeIndex) {
        links.forEach((link) => {
          if (sceneIndex(link.hash) === nextIndex) link.setAttribute("aria-current", "step");
          else link.removeAttribute("aria-current");
        });
        activeIndex = nextIndex;
      }
      previousProgress = progress;
    };

    function scheduleRender() {
      if (motion.matches && frame === 0) frame = window.requestAnimationFrame(renderStory);
    }

    function scheduleHashAlignment() {
      if (!motion.matches) return;
      alignHash = true;
      scheduleRender();
    }

    function followStoryLink(event: MouseEvent) {
      if (!motion.matches || event.defaultPrevented || event.button !== 0 ||
          event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || !(event.target instanceof Element)) return;

      const link = event.target.closest("a[href^='#']");
      if (!(link instanceof HTMLAnchorElement) || link.hasAttribute("download") || (link.target && link.target !== "_self")) return;

      const index = sceneIndex(link.hash);
      if (index < 0) return;

      event.preventDefault();
      alignHash = false;
      if (window.location.hash !== link.hash) window.history.pushState(null, "", link.hash);
      scrollToScene(index, "smooth");
    }

    const resetStory = () => {
      window.cancelAnimationFrame(frame);
      frame = 0;
      alignHash = false;
      previousProgress = -1;
      activeIndex = -1;
      story.removeAttribute("data-story-ready");
      scenes.forEach((scene) => {
        scene.removeAttribute("aria-hidden");
        scene.style.removeProperty("--scene-opacity");
        scene.style.removeProperty("--scene-copy-opacity");
        scene.style.removeProperty("--scene-offset");
        scene.style.removeProperty("--scene-scale");
      });
      links.forEach((link) => link.removeAttribute("aria-current"));
    };

    const syncMotion = () => {
      if (!motion.matches) {
        resetStory();
        return;
      }

      story.setAttribute("data-story-ready", "");
      renderStory();
    };

    syncMotion();
    motion.addEventListener("change", syncMotion);
    window.addEventListener("scroll", scheduleRender, { passive: true });
    window.addEventListener("resize", scheduleRender);
    window.addEventListener("popstate", scheduleHashAlignment);
    window.addEventListener("hashchange", scheduleHashAlignment);
    landing.addEventListener("click", followStoryLink);

    return () => {
      resetStory();
      motion.removeEventListener("change", syncMotion);
      window.removeEventListener("scroll", scheduleRender);
      window.removeEventListener("resize", scheduleRender);
      window.removeEventListener("popstate", scheduleHashAlignment);
      window.removeEventListener("hashchange", scheduleHashAlignment);
      landing.removeEventListener("click", followStoryLink);
    };
  }, []);

  return <main ref={root} className={styles.landing}>{children}</main>;
}
