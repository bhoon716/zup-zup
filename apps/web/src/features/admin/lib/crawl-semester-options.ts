export interface CrawlSemesterOption {
  code: string;
  label: string;
}

export const CRAWL_SEMESTER_OPTIONS: CrawlSemesterOption[] = [
  { code: "U211600010", label: "1학기" },
  { code: "U211600020", label: "2학기" },
  { code: "U211600015", label: "하기 계절학기" },
  { code: "U211600025", label: "동기 계절학기" },
  { code: "U211600016", label: "여름 특별학기" },
  { code: "U211600026", label: "겨울 특별학기" },
  { code: "U211600009", label: "신입생 특별학기" },
  { code: "U211600008", label: "SW 특별학기" },
];
