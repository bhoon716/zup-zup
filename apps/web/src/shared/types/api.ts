







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



export type CourseClassification = '계열공통' | '교양' | '교직(대학원)' | '교직(대)' | '교직' | '군사학' | '기초필수' | '선수' | '일반선택' | '전공' | '전공(대학원)' | '전공(대)' | '전공선택' | '전공필수';
export type GradingMethod = 'Pass/Fail' | '기타(법전원)' | '상대평가Ⅰ' | '상대평가Ⅱ' | '상대평가Ⅲ' | '절대평가';
export type LectureLanguage = '한국어' | '영어' | '독일어' | '스페인어' | '일본어' | '중국어' | '프랑스어';
export type CourseDayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
  | 'MO' | 'TU' | 'WE' | 'TH' | 'FR' | 'SA' | 'SU'
  | '월' | '화' | '수' | '목' | '금' | '토' | '일';




export interface Course {
  courseKey: string;
  subjectCode: string;
  stdtrNo?: string | null;
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

  lectureHours?: number;
  hasSyllabus?: boolean;
  generalCategoryByYear?: string;
  courseDirection?: string;
  classDuration?: string;

  generalCategory?: string;
  generalDetail?: string;
  accreditation?: string;
  courseStatus?: string;
  classroom?: string;
  disclosure?: string;
  disclosureReason?: string;
  lastCrawledAt?: string;
  isSubscribable?: boolean;

  totalSeats?: number;
  currentSeats?: number;
  professorName?: string;
  status?: string;
  averageRating?: number;
  reviewCount?: number;
  isReviewed?: boolean;
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




export interface CourseSearchCondition {
  academicYear?: string;
  semester?: string;
  name?: string;
  keyword?: string;
  professor?: string;
  classifications?: CourseClassification[];
  department?: string;
  gradingMethods?: GradingMethod[];
  subjectCode?: string;
  lectureLanguages?: LectureLanguage[];
  isAvailableOnly?: boolean;
  dayOfWeek?: CourseDayOfWeek;
  selectedSchedules?: ScheduleCondition[];
  credits?: string[];
  lectureHours?: number;
  minLectureHours?: number;
  generalCategory?: string;
  generalDetail?: string;
  isWishedOnly?: boolean;
  statuses?: string[];
  minCredits?: number;
  targetGrades?: string[];
  disclosure?: string;
  courseDirection?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  collegeId?: number;
  departmentId?: number;
}

export interface CourseCategoryResponse {
  category: string;
  details: string[];
}

export interface CollegeHierarchyResponse {
  id: number;
  name: string;
  departments: {
    id: number;
    name: string;
  }[];
}

export interface CourseSeatHistory {
  id: number;
  courseKey: string;
  capacity: number;
  current: number;
  detectedAt: string;
}





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


export interface EmailRequest {
  email: string;
}

export interface EmailVerificationRequest {
  email: string;
  code: string;
}


export interface NotificationHistory {
  id: number;
  courseKey: string;
  title: string;
  message: string;
  channel?: 'FCM' | 'EMAIL' | 'WEB' | 'DISCORD';
  sentAt?: string;
  createdAt?: string;
}


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

export interface AdminDashboardSnapshotResponse {
  overview: AdminOverviewResponse;
  crawlTarget: AdminCrawlTargetResponse;
}

export type NotificationDeliveryChannel = 'EMAIL' | 'FCM' | 'WEB' | 'DISCORD';
export type NotificationDeliveryStatus = 'PENDING' | 'PROCESSING' | 'SENT' | 'DLQ';




export interface AdminNotificationDeliveryResponse {
  id: number;
  outboxId: number;
  courseKey: string;
  courseName: string;
  channel: NotificationDeliveryChannel;
  status: NotificationDeliveryStatus;
  attempts: number;
  lastError: string | null;
  lastErrorReason: string | null;
  deadLetteredAt: string | null;
  idempotencyKeyRetained: boolean;
}

export interface NotificationDeliveryReplayRequest {
  forceSentReplay?: boolean;
}




export interface AdminCrawlTargetRequest {
  year: string;
  semester: string;
}




export interface AdminCrawlTargetResponse {
  year: string;
  semester: string;
}




export interface SearchDefaultSemesterResponse {
  semester: string;
}


export interface CourseSearchPageResponse {
  content: Course[];
  last: boolean;
  number: number;
}


export interface DashboardSnapshotResponse {
  user: User | null;
  notifications: NotificationHistory[];
  primaryTimetable: TimetableDetailResponse | null;
  upcomingSchedules: ScheduleResponse[];
  announcements: AnnouncementListItemResponse[];
}


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
  courseCount?: number;
}

export interface TimetableRequest {
  name: string;
  isPrimary?: boolean;
  primary?: boolean;
}

export interface CustomScheduleTimeRequest {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  classroom?: string;
}

export interface CustomScheduleRequest {
  title: string;
  professor?: string;
  schedules: CustomScheduleTimeRequest[];
}


export interface ScheduleResponse {
  id: number;
  scheduleType: string;
  startDate: string;
  endDate: string;
  startTime?: string | null;
  endTime?: string | null;
  dDay: string;
  ddayEnabled: boolean;
  deletedAt: string | null;
}

export interface ScheduleRequest {
  scheduleType: string;
  startDate: string;
  endDate: string;
  startTime?: string;
  endTime?: string;
  ddayEnabled?: boolean;
}


export type AnnouncementSearchType = "TITLE" | "CONTENT" | "TITLE_CONTENT";

export interface AnnouncementListItemResponse {
  id: number;
  title: string;
  previewContent: string;
  pinned: boolean;
  createdAt: string;
}

export interface AnnouncementDetailResponse {
  id: number;
  title: string;
  content: string;
  pinned: boolean;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AnnouncementRequest {
  title: string;
  content: string;
  pinned?: boolean;
  published?: boolean;
}


export interface ReviewResponse {
  id: number;
  courseKey: string;
  rating: number;
  likeCount: number;
  dislikeCount: number;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewCreateRequest {
  rating: number;
}

export interface ReviewUpdateRequest {
  rating: number;
}

export interface ReviewReactionRequest {
  reactionType: 'LIKE' | 'DISLIKE';
}


export interface EmojiReviewResponse {
  emoji: string;
  count: number;
  isMine: boolean;
}


export type FeedbackType = 'BUG' | 'SUGGESTION' | 'OTHER';
export type FeedbackStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface FeedbackResponse {
  id: number;
  type: FeedbackType;
  title: string;
  status: FeedbackStatus;
  createdAt: string;
  hasReplies: boolean;
}

export interface FeedbackDetailResponse {
  id: number;
  type: FeedbackType;
  title: string;
  content: string;
  status: FeedbackStatus;
  metaInfo: string;
  createdAt: string;
  imageUrls: string[];
  replies: FeedbackReplyResponse[];
}

export interface FeedbackReplyResponse {
  id: number;
  adminName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export type AdminFeedbackDeletionFilter = 'ALL' | 'ACTIVE' | 'DELETED';

export interface AdminFeedbackResponse {
  id: number;
  type: FeedbackType;
  title: string;
  status: FeedbackStatus;
  createdAt: string;
  hasReplies: boolean;
  deleted: boolean;
  deletedAt: string | null;
  authorLabel: string;
}

export interface AdminFeedbackAttachmentResponse {
  id: number;
}

export interface AdminFeedbackReplyResponse {
  id: number;
  authorLabel: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
  deletedAt: string | null;
}

export interface AdminFeedbackDetailResponse {
  id: number;
  type: FeedbackType;
  title: string;
  content: string;
  status: FeedbackStatus;
  createdAt: string;
  deleted: boolean;
  deletedAt: string | null;
  authorLabel: string;
  attachments: AdminFeedbackAttachmentResponse[];
  replies: AdminFeedbackReplyResponse[];
}

export interface FeedbackCreateRequest {
  type: FeedbackType;
  title: string;
  content: string;
  metaInfo: string;
}

export interface FeedbackStatusUpdateRequest {
  status: FeedbackStatus;
}

export interface FeedbackReplyCreateRequest {
  content: string;
}

export interface FeedbackReplyUpdateRequest {
  content: string;
}
