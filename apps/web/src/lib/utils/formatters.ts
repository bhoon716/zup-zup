
/**
 * API 데이터 값 국문 변환 유틸리티
 */

// 이수구분 매핑
const CLASSIFICATION_MAP: Record<string, string> = {
  // Standard Codes (Prediction)
  'MAJOR_REQUIRED': '전공필수',
  'MAJOR_SELECTIVE': '전공선택',
  'MAJOR_ELECTIVE': '전공선택', // User reported code
  'MAJOR': '전공',
  'GENERAL_REQUIRED': '교양필수',
  'GENERAL_SELECTIVE': '교양선택',
  'GENERAL': '교양',
  'LIBERAL_ARTS': '교양',
  'TEACHING': '교직',
  'TEACHING_REQUIRED': '교직필수',
  'TEACHING_SELECTIVE': '교직선택',
  'OPTIONAL': '자유선택', // or 일반선택
  
  // Existing Korean Identity
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

// 강의언어 매핑
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
  
  // Identity
  '한국어': '한국어',
  '영어': '영어',
  '독일어': '독일어',
  '스페인어': '스페인어',
  '일본어': '일본어',
  '중국어': '중국어',
  '프랑스어': '프랑스어',
};

// 성적평가방식 매핑
const GRADING_MAP: Record<string, string> = {
  'ABSOLUTE': '절대평가',
  'RELATIVE': '상대평가',
  'RELATIVE_1': '상대평가Ⅰ',
  'RELATIVE_2': '상대평가Ⅱ',
  'RELATIVE_3': '상대평가Ⅲ',
  'P_F': 'Pass/Fail',
  'PASS_FAIL': 'Pass/Fail',
  
  // Identity
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
