// 공통 응답 타입
export interface CommonResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

// 페이지네이션
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
// Enum-like string unions matching backend
export type CourseClassification = '계열공통' | '교양' | '교직(대)' | '교직' | '군사학' | '기초필수' | '선수' | '일반선택' | '전공' | '전공선택' | '전공필수';
export type GradingMethod = 'Pass/Fail' | '기타(법전원)' | '상대평가Ⅰ' | '상대평가Ⅱ' | '상대평가Ⅲ' | '절대평가';
export type LectureLanguage = '한국어' | '영어' | '독일어' | '스페인어' | '일본어' | '중국어' | '프랑스어';
export type CourseDayOfWeek = '월' | '화' | '수' | '목' | '금' | '토' | '일';
export type ClassPeriod = '0-A' | '0-B' | '1-A' | '1-B' | '2-A' | '2-B' | '3-A' | '3-B' | '4-A' | '4-B' | '5-A' | '5-B' | '6-A' | '6-B' | '7-A' | '7-B' | '8-A' | '8-B' | '9-A' | '9-B' | '10-A' | '10-B' | '11-A' | '11-B' | '12-A' | '12-B' | '13-A' | '13-B' | '14-A' | '14-B' | '15-A' | '15-B';

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
  // New fields
  lectureHours?: number;
  hasSyllabus?: boolean;
  generalCategoryByYear?: string;
  courseDirection?: string;
  classDuration?: string;
  // added fields
  generalCategory?: string;
  generalDetail?: string;
  accreditation?: string;
  courseStatus?: string;
  classroom?: string;
  disclosure?: string;
  disclosureReason?: string;
  lastCrawledAt?: string;
  // Legacy/Compatibility fields from OpenAPI spec
  totalSeats?: number;
  currentSeats?: number;
  professorName?: string;
  status?: string;
  schedules?: {
    dayOfWeek: CourseDayOfWeek;
    period: ClassPeriod;
  }[];
}

export interface ScheduleCondition {
  dayOfWeek: CourseDayOfWeek;
  period: ClassPeriod;
}

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
  period?: ClassPeriod;
  selectedSchedules?: ScheduleCondition[];
  credits?: string;
  lectureHours?: number;
  minLectureHours?: number;
  generalCategory?: string;
  generalDetail?: string;
}

export interface CourseCategoryResponse {
  category: string;
  details: string[];
}

export interface CourseSeatHistory {
  id: number;
  courseKey: string;
  currentSeats: number;
  changedSeats: number;
  createdAt: string;
}

// 구독 관련
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
export interface User {
  id: number;
  email: string;
  name: string;
  role: string;
  notificationEmail?: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  fcmEnabled: boolean;
  onboardingCompleted: boolean;
}

export interface OnboardingRequest {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
}

export interface UserSettingsRequest {
  notificationEmail?: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  fcmEnabled: boolean;
}

export interface UserUpdateRequest {
  name: string;
}

// Email Verification
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
  createdAt: string;
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
}

export interface UserUpdateRequest {
  name: string;
}

export interface AdminDashboardResponse {
  totalUsers: number;
  totalActiveSubscriptions: number;
  todayNotificationCount: number;
  crawlingStatus: string;
  lastCrawledAt: string;
}

// 찜(Wishlist) 관련
export interface WishlistResponse {
  id: number;
  userId: number;
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

// 시간표(Timetable) 관련
export interface TimetableEntryResponse {
  id: number;
  courseKey: string;
  courseName: string;
  professor: string;
  schedules: {
    dayOfWeek: CourseDayOfWeek;
    period: ClassPeriod;
    startTime: string;
    endTime: string;
  }[];
}

export interface CustomScheduleResponse {
  id: number;
  title: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  color: string;
}

export interface TimetableResponse {
  id: number;
  name: string;
  primary: boolean;
  courses: TimetableEntryResponse[];
  customSchedules: CustomScheduleResponse[];
}

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
}

export interface TimetableRequest {
  name: string;
  primary: boolean;
}

export interface CustomScheduleRequest {
  title: string;
  dayOfWeek: string;
  startTime: string; // "HH:mm"
  endTime: string;   // "HH:mm"
  color: string;
}
