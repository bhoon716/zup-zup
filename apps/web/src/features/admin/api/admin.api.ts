import api from "@/shared/api/client";
import type {
  CommonResponse,
  AdminDashboardResponse,
  AdminOverviewResponse,
  AdminCrawlTargetRequest,
  AdminCrawlTargetResponse,
  AnnouncementRequest,
  AnnouncementDetailResponse,
} from '@/shared/types/api';

/**
 * 관리자 대시보드 통계 데이터를 조회합니다.
 */
export const getDashboardStats = async (): Promise<CommonResponse<AdminDashboardResponse>> => {
  const { data } = await api.get('/api/v1/admin/stats');
  return data;
};

/**
 * 관리자 대시보드 요약 정보를 조회합니다.
 */
export const getDashboardOverview = async (): Promise<CommonResponse<AdminOverviewResponse>> => {
  const { data } = await api.get('/api/v1/admin/overview');
  return data;
};

/**
 * 기본 타겟으로 강의 크롤링을 즉시 실행합니다.
 */
export const crawlCourses = async (): Promise<CommonResponse<string>> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl');
  return data;
};

/**
 * 특정 년도와 학기를 대상으로 강의 크롤링을 즉시 실행합니다.
 */
export const crawlCoursesByTarget = async (
  request: AdminCrawlTargetRequest
): Promise<CommonResponse<string>> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl/target', request);
  return data;
};

/**
 * 현재 설정된 기본 크롤링 타겟(년도, 학기) 정보를 조회합니다.
 */
export const getCrawlTarget = async (): Promise<CommonResponse<AdminCrawlTargetResponse>> => {
  const { data } = await api.get('/api/v1/admin/courses/crawl-target');
  return data;
};

/**
 * 스케줄러 등에서 사용할 기본 크롤링 타겟을 수정합니다.
 */
export const updateCrawlTarget = async (
  request: AdminCrawlTargetRequest
): Promise<CommonResponse<AdminCrawlTargetResponse>> => {
  const { data } = await api.put('/api/v1/admin/courses/crawl-target', request);
  return data;
};

/**
 * 관리자용 테스트 알림을 발송합니다.
 */
export const sendTestNotification = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/admin/notifications/test');
  return data;
};

/**
 * 관리자 공지사항 목록을 조회합니다. (공개/비공개 포함)
 */
export const getAdminAnnouncements = async (): Promise<CommonResponse<AnnouncementDetailResponse[]>> => {
  const { data } = await api.get('/api/v1/admin/announcements');
  return data;
};

/**
 * 관리자 공지사항을 생성합니다.
 */
export const createAdminAnnouncement = async (
  request: AnnouncementRequest
): Promise<CommonResponse<AnnouncementDetailResponse>> => {
  const { data } = await api.post('/api/v1/admin/announcements', request);
  return data;
};

/**
 * 관리자 공지사항을 수정합니다.
 */
export const updateAdminAnnouncement = async (
  id: number,
  request: AnnouncementRequest
): Promise<CommonResponse<AnnouncementDetailResponse>> => {
  const { data } = await api.put(`/api/v1/admin/announcements/${id}`, request);
  return data;
};

/**
 * 관리자 공지사항을 삭제합니다.
 */
export const deleteAdminAnnouncement = async (id: number): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/admin/announcements/${id}`);
  return data;
};
