// 공통 응답 타입
export interface CommonResponse<T = any> {
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

// 강좌 관련
export interface Course {
  courseKey: string;
  subjectCode: string;
  name: string;
  professorName: string;
  targetGrade: string;
  totalSeats: number;
  currentSeats: number;
  status: 'AVAILABLE' | 'FULL';
}

export interface CourseSearchCondition {
  name?: string;
  professor?: string;
  subjectCode?: string;
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
  subjectName: string;
  isActive: boolean;
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
export interface UserDeviceRequest {
  type: 'FCM' | 'WEB';
  token: string;
  p256dh?: string;
  auth?: string;
}
