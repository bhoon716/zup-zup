"use client";

import { TimetablePage } from "./timetable-page";
import { useTimetablePage } from "./useTimetablePage";

export default function TimetableRoutePage() {
  return <TimetablePage model={useTimetablePage()} />;
}
