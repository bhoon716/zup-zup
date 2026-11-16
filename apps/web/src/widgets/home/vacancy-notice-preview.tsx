import { BatteryFull, LockKeyhole, Signal, Wifi } from "lucide-react";
import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import { BRAND_ICON_32_PATH } from "@/shared/seo/branding";
import styles from "./vacancy-notice.module.css";

const NOTICE_VISIBILITY_THRESHOLD = 0.35;

const exampleNotices = [
  { arrival: "latest", course: "운영체제 02분반", seats: 2, received: "지금" },
  { arrival: "first", course: "자료구조 01분반", seats: 1, received: "방금" },
];

export function VacancyNoticePreview() {
  const sceneRef = useRef<HTMLDivElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  useEffect(() => {
    const scene = sceneRef.current;
    if (!scene) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) {
            setIsPlaying(false);
          } else if (entry.intersectionRatio >= NOTICE_VISIBILITY_THRESHOLD) {
            setIsPlaying(true);
          }
        }
      },
      { threshold: [0, NOTICE_VISIBILITY_THRESHOLD] },
    );
    observer.observe(scene);

    return () => observer.disconnect();
  }, []);

  return (
    <figure className={styles.preview} aria-label="빈자리 알림 예시" data-testid="landing-vacancy-preview">
      <div ref={sceneRef} className={styles.scene} data-vacancy-scene data-vacancy-playing={isPlaying}>
        <div className={styles.phone}>
          <div className={styles.screen}>
            <div className={styles.wallpaper} data-vacancy-wallpaper aria-hidden="true" />
            <div className={styles.status} aria-hidden="true">
              <span className={styles.cutout} />
              <div className={styles.statusIcons}>
                <Signal />
                <Wifi />
                <BatteryFull />
              </div>
            </div>

            <div className={styles.lockScreen}>
              <div className={styles.clock} aria-hidden="true">
                <LockKeyhole />
                <span className={styles.date}>3월 3일 화요일</span>
                <span className={styles.time}>9:41</span>
              </div>

              <ul className={styles.notifications}>
                {exampleNotices.map(({ arrival, course, seats, received }) => (
                  <li key={course} className={styles.notification} data-vacancy-notice={arrival}>
                    <div className={styles.notificationMeta}>
                      <Image src={BRAND_ICON_32_PATH} alt="" width={16} height={16} />
                      <span>줍줍</span>
                      <span className={styles.received}>{received}</span>
                    </div>
                    <p className={styles.notificationTitle}>빈자리가 생겼어요!</p>
                    <p className={styles.course}>{course} · 여석 {seats}석</p>
                  </li>
                ))}
              </ul>
            </div>

            <span className={styles.homeIndicator} aria-hidden="true" />
          </div>
        </div>
      </div>
    </figure>
  );
}
