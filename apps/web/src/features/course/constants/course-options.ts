import { CourseClassification, GradingMethod, LectureLanguage, CourseDayOfWeek } from "@/shared/types/api";

export const DEFAULT_CONDITION = {
  academicYear: "2026",
  semester: "U211600010",
};

export const CLASSIFICATION_GROUPS = [
  {
    label: "전공(대학원)",
    items: ["전공", "전공필수", "전공선택"],
  },
  {
    label: "교양",
    items: ["교양"],
    value: "general", // "general" type for special UI
  },
  {
    label: "일반선택",
    items: ["일반선택"],
    value: "elective",
  },
  {
    label: "기타",
    items: ["기초필수", "계열공통", "교직", "교직(대)", "군사학", "선수", "논문"],
  },
];

export const GRADING_GROUPS = [
  {
    label: "상대평가",
    items: ["상대평가Ⅰ", "상대평가Ⅱ", "상대평가Ⅲ"],
  },
  {
    label: "절대평가/P.F",
    items: ["절대평가", "Pass/Fail"],
  },
  {
    label: "기타",
    items: ["기타(법전원)"],
  },
];

export const LANGUAGES: LectureLanguage[] = [
  "한국어",
  "영어",
  "독일어",
  "스페인어",
  "일본어",
  "중국어",
  "프랑스어",
];

export const CREDITS = ["0.5", "1", "2", "3", "4+"];
export const TARGET_GRADES = ["1", "2", "3", "4", "5", "6", "GRADUATE"];
export const DISCLOSURES = ["공개", "비공개"];
export const COURSE_DIRECTIONS = [
  "일반",
  "원격강좌(콘텐츠)",
  "원격강좌(실시간)",
  "플립러닝",
  "블렌디드러닝",
  "온·오프라인강좌",
  "현장실습",
  "사회봉사",
  "논문연구",
  "화상강의",
  "특별(영어)",
];

export const YEARS = ["2026", "2025", "2024"];

export const SEMESTERS = [
  { label: "1학기", value: "U211600010" },
  { label: "계절학기(하기)", value: "U010200021" },
  { label: "2학기", value: "U211600020" },
  { label: "계절학기(동기)", value: "U010200022" },
  { label: "특별학기(여름)", value: "S1" },
  { label: "특별학기(겨울)", value: "S2" },
  { label: "특별학기(신입생)", value: "S3" },
  { label: "특별학기(SW)", value: "S4" },
];

export const GE_CATEGORIES: Record<string, string[]> = {
  균형교양: ["AI·SW", "글로컬", "삶과사회", "예술과체육", "외국어", "인간과문화", "자연과과학"],
  기초교양: ["기초필수", "문제해결", "의사소통"],
  기타교양: ["타대학개설교양", "예비학기제교양"],
  진로: ["진로"],
};

export const SMART_FILTER_DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
export const SMART_FILTER_START_MINUTES = 9 * 60;
export const SMART_FILTER_SLOT_MINUTES = 60;
export const SMART_FILTER_SLOT_COUNT = 13;
