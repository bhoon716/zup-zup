// 공통 응답 타입
export interface CommonResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

// 분할 페이지 응답
export interface SliceResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

// 페이지 응답(관리자/이전 형식)
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

// 강의 관련
// 서버 열거형 문자열과 일대일 매핑
export type CourseClassification = '계열공통' | '교양' | '교직(대학원)' | '교직(대)' | '교직' | '군사학' | '기초필수' | '선수' | '일반선택' | '전공' | '전공선택' | '전공필수';
export type GradingMethod = 'Pass/Fail' | '기타(법전원)' | '상대평가Ⅰ' | '상대평가Ⅱ' | '상대평가Ⅲ' | '절대평가';
export type LectureLanguage = '한국어' | '영어' | '독일어' | '스페인어' | '일본어' | '중국어' | '프랑스어';
export type CourseDayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
  | 'MO' | 'TU' | 'WE' | 'TH' | 'FR' | 'SA' | 'SU'
  | '월' | '화' | '수' | '목' | '금' | '토' | '일';

/**
 * 강의 기본 정보 인터페이스
 */
export interface Course {
  courseKey: string;
  subjectCode: string;
  name: string;
  classNumber: string;
  professor?: string;
  capacity?: number;
  current?: number;
  available?: number;
  targetGrade?: string;
  academicYear?: string;
  semester?: string;
  classification?: CourseClassification;
  department?: string;
  gradingMethod?: GradingMethod;
  lectureLanguage?: LectureLanguage;
  classTime?: string;
  credits?: string;
  // 확장 필드
  lectureHours?: number;
  hasSyllabus?: boolean;
  generalCategoryByYear?: string;
  courseDirection?: string;
  classDuration?: string;
  // 교양/공개 설정 관련 필드
  generalCategory?: string;
  generalDetail?: string;
  accreditation?: string;
  courseStatus?: string;
  classroom?: string;
  disclosure?: string;
  disclosureReason?: string;
  lastCrawledAt?: string;
  // 명세 호환 필드
  totalSeats?: number;
  currentSeats?: number;
  professorName?: string;
  status?: string;
  schedules?: {
    dayOfWeek: CourseDayOfWeek;
    startTime: string;
    endTime: string;
  }[];
}

export interface ScheduleCondition {
  dayOfWeek: CourseDayOfWeek;
  startTime: string;
  endTime: string;
}

/**
 * 강의 검색 조건 인터페이스
 */
export interface CourseSearchCondition {
  academicYear?: string;
  semester?: string;
  name?: string;
  professor?: string;
  classification?: CourseClassification;
  department?: string;
  gradingMethod?: GradingMethod;
  subjectCode?: string;
  lectureLanguage?: LectureLanguage;
  isAvailableOnly?: boolean;
  dayOfWeek?: CourseDayOfWeek;
  selectedSchedules?: ScheduleCondition[];
  credits?: string;
  lectureHours?: number;
  minLectureHours?: number;
  generalCategory?: string;
  generalDetail?: string;
  isWishedOnly?: boolean;
  status?: string;
  minCredits?: number;
  targetGrade?: string;
  disclosure?: string;
  courseDirection?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface CourseCategoryResponse {
  category: string;
  details: string[];
}

export interface CourseSeatHistory {
  id: number;
  courseKey: string;
  capacity: number;
  current: number;
  detectedAt: string;
}

// 구독 관련
/**
 * 여석 알림 구독 정보 인터페이스
 */
export interface Subscription {
  id: number;
  courseKey: string;
  courseName: string;
  professorName: string;
  isActive: boolean;
  createdAt: string;
}

export interface SubscriptionRequest {
  courseKey: string;
}

// 사용자 관련
/**
 * 사용자 정보 인터페이스
 */
export interface User {
  id: number;
  email: string;
  name: string;
  role: string;
  notificationEmail?: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  fcmEnabled: boolean;
  discordEnabled: boolean;
  discordId?: string;
  onboardingCompleted: boolean;
}

export interface OnboardingRequest {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  fcmEnabled: boolean;
  discordEnabled?: boolean;
}

export interface UserSettingsRequest {
  notificationEmail?: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  fcmEnabled: boolean;
  discordEnabled: boolean;
}

export interface UserUpdateRequest {
  name: string;
}

// 이메일 인증
export interface EmailRequest {
  email: string;
}

export interface EmailVerificationRequest {
  email: string;
  code: string;
}

// 알림 관련
export interface NotificationHistory {
  id: number;
  courseKey: string;
  title: string;
  message: string;
  channel?: 'FCM' | 'EMAIL' | 'WEB' | 'DISCORD';
  sentAt?: string;
  createdAt?: string;
}

// 기기 등록 관련
export interface UserDevice {
  id: number;
  type: 'FCM' | 'WEB';
  token: string;
  p256dh?: string;
  auth?: string;
  isActive: boolean;
  createdAt: string;
}

export interface UserDeviceRequest {
  type: 'FCM' | 'WEB';
  token: string;
  p256dh?: string;
  auth?: string;
  alias?: string;
}

export interface UserDeviceResponse {
  id: number;
  type: 'FCM' | 'WEB';
  alias?: string;
  maskedToken: string;
  registeredAt: string;
}

export interface AdminDashboardResponse {
  totalUsers: number;
  totalActiveSubscriptions: number;
  todayNotificationCount: number;
  crawlingStatus: string;
  lastCrawledAt: string | null;
}

export interface AdminTrafficPointResponse {
  label: string;
  count: number;
}

export interface AdminRecentLogResponse {
  timestamp: string;
  level: string;
  message: string;
  source: string;
}

export interface AdminOverviewResponse {
  totalUsers: number;
  totalActiveSubscriptions: number;
  todayNotificationCount: number;
  crawlingStatus: string;
  lastCrawledAt: string | null;
  jbnuLatencyMs: number | null;
  serverTime: string;
  notificationTraffic: AdminTrafficPointResponse[];
  recentLogs: AdminRecentLogResponse[];
}

// 찜 관련
export interface WishlistResponse {
  id: number;
  userId?: number;
  courseKey: string;
  courseName: string;
  professor: string;
  classification: string;
  credits: string;
  classTime: string;
  capacity: number;
  current: number;
  available: number;
  subjectCode: string;
  classNumber: string;
  createdAt: string;
}

export interface WishlistToggleResponse {
  isWished: boolean;
}

// 시간표 관련
export interface TimetableEntryResponse {
  courseKey: string;
  name: string;
  professor: string;
  classTime: string;
  credits: string;
  classification: string;
  classroom: string;
  schedules: {
    dayOfWeek: CourseDayOfWeek;
    startTime: string;
    endTime: string;
  }[];
}

export interface CustomScheduleTimeResponse {
  id: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  classroom?: string;
}

export interface CustomScheduleResponse {
  id: number;
  title: string;
  professor?: string;
  schedules: CustomScheduleTimeResponse[];
}

/**
 * 시간표 요약 정보 인터페이스
 */
export interface TimetableResponse {
  id: number;
  name: string;
  primary: boolean;
  courses: TimetableEntryResponse[];
  customSchedules: CustomScheduleResponse[];
}

/**
 * 시간표 상세 정보 인터페이스
 */
export interface TimetableDetailResponse {
  id: number;
  name: string;
  primary: boolean;
  courses: TimetableEntryResponse[];
  customSchedules: CustomScheduleResponse[];
  totalCredits: string;
}

export interface TimetableListResponse {
  id: number;
  name: string;
  primary: boolean;
  courseCount?: number;
}

export interface TimetableRequest {
  name: string;
  isPrimary?: boolean;
  primary?: boolean;
}

export interface CustomScheduleTimeRequest {
  dayOfWeek: string;
  startTime: string; // 시:분:초 형식 (서버 LocalTime 호환)
  endTime: string;   // 시:분:초 형식
  classroom?: string;
}

export interface CustomScheduleRequest {
  title: string;
  professor?: string;
  schedules: CustomScheduleTimeRequest[];
}
