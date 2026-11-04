import api from "@/shared/api/client";
import type { AdminDashboardSnapshotResponse, AdminCrawlTargetRequest, AdminCrawlTargetResponse, AdminNotificationDeliveryResponse, AnnouncementRequest, AnnouncementDetailResponse, NotificationDeliveryReplayRequest, PageResponse } from '@/shared/types/api';




export const getDashboardSnapshot = async (): Promise<AdminDashboardSnapshotResponse> => {
  const { data } = await api.get('/api/v1/admin/dashboard');
  return data;
};




export const crawlCourses = async (): Promise<string> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl');
  return data;
};




export const crawlCoursesByTarget = async (
  request: AdminCrawlTargetRequest
): Promise<string> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl/target', request);
  return data;
};




export const getCrawlTarget = async (): Promise<AdminCrawlTargetResponse> => {
  const { data } = await api.get('/api/v1/admin/courses/crawl-target');
  return data;
};




export const updateCrawlTarget = async (
  request: AdminCrawlTargetRequest
): Promise<AdminCrawlTargetResponse> => {
  const { data } = await api.put('/api/v1/admin/courses/crawl-target', request);
  return data;
};




export const sendTestNotification = async (): Promise<void> => {
  const { data } = await api.post('/api/v1/admin/notifications/test');
  return data;
};




export const getAdminDlqNotificationDeliveries = async (
  page: number = 0,
): Promise<PageResponse<AdminNotificationDeliveryResponse>> => {
  const { data } = await api.get<PageResponse<AdminNotificationDeliveryResponse>>(
    '/api/v1/admin/notification-deliveries/dlq',
    { params: { page } },
  );
  return data;
};




export const getAdminNotificationDelivery = async (
  deliveryId: number,
): Promise<AdminNotificationDeliveryResponse> => {
  const { data } = await api.get<AdminNotificationDeliveryResponse>(
    `/api/v1/admin/notification-deliveries/${deliveryId}`,
  );
  return data;
};




export const replayAdminNotificationDelivery = async (
  deliveryId: number,
  request: NotificationDeliveryReplayRequest = {},
): Promise<AdminNotificationDeliveryResponse> => {
  const { data } = await api.post<AdminNotificationDeliveryResponse>(
    `/api/v1/admin/notification-deliveries/${deliveryId}/replay`,
    request,
  );
  return data;
};




export const getAdminAnnouncements = async (
  page: number = 0,
): Promise<PageResponse<AnnouncementDetailResponse>> => {
  const { data } = await api.get('/api/v1/admin/announcements', { params: { page } });
  return data;
};




export const createAdminAnnouncement = async (
  request: AnnouncementRequest
): Promise<AnnouncementDetailResponse> => {
  const { data } = await api.post('/api/v1/admin/announcements', request);
  return data;
};




export const updateAdminAnnouncement = async (
  id: number,
  request: AnnouncementRequest
): Promise<AnnouncementDetailResponse> => {
  const { data } = await api.put(`/api/v1/admin/announcements/${id}`, request);
  return data;
};




export const deleteAdminAnnouncement = async (id: number): Promise<void> => {
  const { data } = await api.delete(`/api/v1/admin/announcements/${id}`);
  return data;
};
