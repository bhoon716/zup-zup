import type { InboxItem } from "../api/inbox.api";

export const inboxTypeLabels: Record<InboxItem["type"], string> = {
  SEAT_OPENED: "빈자리",
  FEEDBACK_REPLY: "문의 답변",
  ANNOUNCEMENT: "중요 공지",
  DELIVERY_FAILED: "알림 발송 실패",
  DEVICE_REREGISTRATION: "기기 재등록",
};

export function getInboxItemTitle(item: InboxItem) {
  return item.type === "SEAT_OPENED" ? item.courseName : item.title ?? inboxTypeLabels[item.type];
}
