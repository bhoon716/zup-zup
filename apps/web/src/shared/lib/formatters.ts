import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

/**
 * 서버 응답 값을 화면용 한글 표현으로 변환한다.
 */

const CLASSIFICATION_MAP: Record<string, string> = {
  'SERIES_COMMON': '계열공통',
  'GENERAL_EDUCATION': '교양',
  'TEACHING_PROFESSION_GRAD': '교직(대)',
  'TEACHING_PROFESSION': '교직',
  'MILITARY_SCIENCE': '군사학',
  'BASIC_REQUIRED': '기초필수',
  'PREREQUISITE': '선수',
  'GENERAL_ELECTIVE': '일반선택',
  'MAJOR': '전공',
  'MAJOR_ELECTIVE': '전공선택',
  'MAJOR_REQUIRED': '전공필수',

  'MAJOR_SELECTIVE': '전공선택',
  'GENERAL_REQUIRED': '교양필수',
  'GENERAL_SELECTIVE': '교양선택',
  'LIBERAL_ARTS': '교양',
  'OPTIONAL': '일반선택',
  '계열공통': '계열공통',
  '교양': '교양',
  '교직(대)': '교직(대)',
  '교직': '교직',
  '군사학': '군사학',
  '기초필수': '기초필수',
  '선수': '선수',
  '일반선택': '일반선택',
  '전공': '전공',
  '전공선택': '전공선택',
  '전공필수': '전공필수',
};

const LANGUAGE_MAP: Record<string, string> = {
  'KO': '한국어',
  'KOREAN': '한국어',
  'EN': '영어',
  'ENGLISH': '영어',
  'DE': '독일어',
  'GERMAN': '독일어',
  'ES': '스페인어',
  'SPANISH': '스페인어',
  'JP': '일본어',
  'JAPANESE': '일본어',
  'CN': '중국어',
  'CHINESE': '중국어',
  'FR': '프랑스어',
  'FRENCH': '프랑스어',
  '한국어': '한국어',
  '영어': '영어',
  '독일어': '독일어',
  '스페인어': '스페인어',
  '일본어': '일본어',
  '중국어': '중국어',
  '프랑스어': '프랑스어',
};

const GRADING_MAP: Record<string, string> = {
  'ABSOLUTE': '절대평가',
  'RELATIVE': '상대평가',
  'RELATIVE_1': '상대평가Ⅰ',
  'RELATIVE_2': '상대평가Ⅱ',
  'RELATIVE_3': '상대평가Ⅲ',
  'P_F': 'Pass/Fail',
  'PASS_FAIL': 'Pass/Fail',
  'Pass/Fail': 'Pass/Fail',
  '기타(법전원)': '기타(법전원)',
  '상대평가Ⅰ': '상대평가Ⅰ',
  '상대평가Ⅱ': '상대평가Ⅱ',
  '상대평가Ⅲ': '상대평가Ⅲ',
  '절대평가': '절대평가',
};

export function formatClassification(value?: string): string {
  if (!value) return '-';
  return CLASSIFICATION_MAP[value] || CLASSIFICATION_MAP[value.toUpperCase()] || value;
}

export function formatLanguage(value?: string): string {
  if (!value) return '-';
  return LANGUAGE_MAP[value] || LANGUAGE_MAP[value.toUpperCase()] || value;
}

export function formatGradingMethod(value?: string): string {
  if (!value) return '-';
  return GRADING_MAP[value] || GRADING_MAP[value.toUpperCase()] || value;
}

export function formatTargetGrade(value?: string): string {
  if (!value) return "-";
  if (value === "ALL" || value === "GRADE_ALL" || value === "전체" || value === "전체학년") return "전체";
  if (value === "GRADUATE" || value === "대학원") return "대학원생";

  const match = value.match(/^(?:GRADE_)?([1-6])$/i);
  if (match) {
    return `${match[1]}학년`;
  }

  return value;
}

/**
 * 상대 시간을 한글 문구로 변환한다.
 */
export function formatRelativeTime(dateString?: string): string {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';
    return formatDistanceToNow(date, { addSuffix: true, locale: ko });
  } catch {
    return '-';
  }
}
