import Link from "next/link";
import { ArrowDown, ArrowRight, Bell, CalendarDays, Check, Search } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { LandingMotion } from "./landing-motion";
import styles from "./home-landing.module.css";

const weekdays = ["월", "화", "수", "목", "금"];
const sampleCourses = [
  { name: "자료구조", meta: "전공필수 · 3학점", time: "화·목 10:00" },
  { name: "알고리즘", meta: "전공선택 · 3학점", time: "월·수 13:00" },
  { name: "인공지능", meta: "전공선택 · 3학점", time: "화·목 15:00" },
];

export function HomeLanding() {
  return (
    <LandingMotion>
      <section className={styles.hero} aria-labelledby="landing-title">
        <div className={`${styles.wrap} ${styles.heroLayout}`}>
          <div className={styles.heroCopy}>
            <p className={styles.eyebrow}>전북대 수강신청 도우미</p>
            <h1 id="landing-title">
              기다리던 빈자리,<br />
              <span>생기면 알려드려요.</span>
            </h1>
            <p className={styles.description}>
              마감된 강의를 계속 확인하느라 하루를 보내지 마세요.
              줍줍에 알림을 걸어두고, 원하는 강의로 시간표를 미리 짜 보세요.
            </p>
            <div className={styles.actions}>
              <Button asChild size="lg" className={styles.primaryAction}>
                <Link href="/login">지금 시작하기 <ArrowRight aria-hidden="true" /></Link>
              </Button>
              <Button asChild size="lg" variant="outline" className={styles.secondaryAction}>
                <Link href="/search">강의 먼저 둘러보기</Link>
              </Button>
            </div>
          </div>

          <figure className={styles.heroFigure}>
            <div className={styles.heroArt}>
              <div className={styles.watchCard}>
                <div className={styles.watchHeader}>
                  <span>관심 강의</span><Bell size={18} aria-hidden="true" />
                </div>
                <div className={styles.watchCourse}>
                  <div><strong>자료구조</strong><span>화·목 10:00 · 3학점</span></div>
                  <span className={styles.closed}>마감</span>
                </div>
                <div className={styles.watchFooter}><Check size={15} aria-hidden="true" /> 빈자리 알림 켜짐</div>
              </div>
              <div className={styles.alertCard}>
                <div className={styles.alertIcon}><Bell size={22} aria-hidden="true" /></div>
                <div><strong>자료구조에 빈자리가 생겼어요</strong><span>빈자리 1석</span></div>
              </div>
            </div>
            <figcaption className={styles.caption}>빈자리 알림 예시</figcaption>
          </figure>

          <a href="#seat-alert" className={styles.explore}><ArrowDown size={16} aria-hidden="true" /> 기능 살펴보기</a>
        </div>
      </section>

      <section id="seat-alert" className={styles.alertSection} aria-labelledby="seat-alert-title">
        <div className={`${styles.wrap} ${styles.feature}`}>
          <div className={styles.copy} data-landing-reveal>
            <p className={styles.eyebrow}><Bell size={18} aria-hidden="true" /> 빈자리 알림</p>
            <h2 id="seat-alert-title">새로고침은 그만.<br />빈자리가 나면 알려드려요.</h2>
            <p className={styles.description}>
              마감된 강의에 알림을 신청하면 빈자리가 확인됐을 때 알림함에 남겨드려요.
              웹 푸시, 이메일, Discord를 연결해 두면 선택한 채널에서도 받을 수 있어요.
            </p>
          </div>
          <ol className={styles.flow} data-landing-reveal>
            <li><span><Search size={19} aria-hidden="true" /></span> 원하는 강의 찾기</li>
            <li><span><Bell size={19} aria-hidden="true" /></span> 빈자리 알림 신청</li>
            <li><span><Check size={19} aria-hidden="true" /></span> 빈자리 알림 받기</li>
          </ol>
        </div>
      </section>

      <section className={styles.searchSection} aria-labelledby="landing-search-title">
        <div className={`${styles.wrap} ${styles.feature} ${styles.reverse}`}>
          <div className={styles.copy} data-landing-reveal>
            <p className={styles.eyebrow}><Search size={18} aria-hidden="true" /> 강의 검색</p>
            <h2 id="landing-search-title">들으려는 강의만<br />빠르게 골라요.</h2>
            <p className={styles.description}>
              강의명이나 교수명으로 찾고, 이수 구분·학점·시간대 조건을 더해 필요한 강의만 모아 보세요.
            </p>
            <Link className={styles.textLink} href="/search">강의 검색 열기 <ArrowRight size={16} aria-hidden="true" /></Link>
          </div>
          <figure className={styles.demoFigure} data-landing-reveal>
            <div className={styles.searchDemo}>
              <div className={styles.searchField}><Search size={18} aria-hidden="true" /> 컴퓨터공학부</div>
              <ul className={styles.filters} aria-label="검색 조건 예시">
                <li>전공</li><li>3학점</li>
              </ul>
              <ul className={styles.courses}>
                {sampleCourses.map((course) => (
                  <li key={course.name}><div><strong>{course.name}</strong><span>{course.meta}</span></div><span>{course.time}</span></li>
                ))}
              </ul>
            </div>
            <figcaption className={styles.caption}>강의 검색 예시</figcaption>
          </figure>
        </div>
      </section>

      <section className={styles.timetableSection} aria-labelledby="landing-timetable-title">
        <div className={`${styles.wrap} ${styles.feature}`}>
          <div className={styles.copy} data-landing-reveal>
            <p className={styles.eyebrow}><CalendarDays size={18} aria-hidden="true" /> 내 시간표</p>
            <h2 id="landing-timetable-title">강의를 담고<br />한 주를 미리 그려요.</h2>
            <p className={styles.description}>
              고민 중인 강의를 시간표에 넣고, 겹치는 시간과 총 학점을 한눈에 확인하세요.
              여러 조합을 저장해 두고 비교할 수 있어요.
            </p>
          </div>
          <figure className={styles.demoFigure} data-landing-reveal>
            <table className={styles.week}>
              <caption>시간표 예시</caption>
              <thead><tr><td /><>{weekdays.map((day) => <th key={day} scope="col">{day}</th>)}</></tr></thead>
              <tbody>
                <tr><th scope="row">10:00</th><td /><td><span>자료구조</span></td><td /><td><span>자료구조</span></td><td /></tr>
                <tr><th scope="row">13:00</th><td><span>알고리즘</span></td><td /><td><span>알고리즘</span></td><td /><td /></tr>
                <tr><th scope="row">15:00</th><td /><td><span>인공지능</span></td><td /><td><span>인공지능</span></td><td /></tr>
              </tbody>
            </table>
          </figure>
        </div>
      </section>

      <section className={styles.closing} aria-labelledby="landing-cta-title">
        <div className={styles.wrap} data-landing-reveal>
          <h2 id="landing-cta-title">듣고 싶은 강의부터 찾아보세요.</h2>
          <p>찾아보고, 알림을 걸고, 시간표에 담아 보세요.</p>
          <Button asChild size="lg" className={styles.primaryAction}>
            <Link href="/search">강의 검색하기 <ArrowRight aria-hidden="true" /></Link>
          </Button>
        </div>
      </section>
    </LandingMotion>
  );
}
